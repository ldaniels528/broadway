package com.github.ldaniels528.broadway.core.io.device.kafka

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.ldaniels528.broadway.core.actors.BroadwayActorSystem
import com.github.ldaniels528.broadway.core.io.Data
import com.github.ldaniels528.broadway.core.io.device.AsynchronousOutputSource
import com.github.ldaniels528.broadway.core.io.device.kafka.KafkaOutputSource.{Die, asyncActor}
import com.github.ldaniels528.broadway.core.io.layout.Layout
import com.github.ldaniels528.broadway.core.scope.Scope
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
  * Kafka Output Source
  *
  * @author lawrence.daniels@gmail.com
  */
case class KafkaOutputSource(id: String, topic: String, zk: ZkProxy, layout: Layout) extends AsynchronousOutputSource {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private val publisher = KafkaPublisher(zk)

  override def allWritesCompleted(implicit scope: Scope, ec: ExecutionContext) = {
    implicit val timeout: Timeout = 1.hour
    (asyncActor ? Die) map { _ =>
      logger.info("Closing Kafka publisher...")
    }
  }

  override def close(scope: Scope) = publisher.close()

  override def open(scope: Scope): Unit = {
    scope ++= Seq(
      "flow.output.topic" -> topic,
      "flow.output.connection" -> zk.connectionString
    )
    publisher.open()
  }

  override def write(scope: Scope, data: Data) = {
    val key = ByteBufferUtils.uuidToBytes(UUID.randomUUID())
    val message = data.asBytes
    implicit val timeout: Timeout = 15.seconds
    (asyncActor ? publisher.publish(topic, key, message)) foreach (_ => updateCount(scope, 1))
    0
  }

}

/**
  * Kafka Output IOSource Companion Object
  */
object KafkaOutputSource {
  val asyncActor = BroadwayActorSystem.system.actorOf(Props[AsyncResultActor])
  var dying = false

  /**
    * Asynchronous Results Actor
    */
  class AsyncResultActor() extends Actor with ActorLogging {
    private val tasks = TrieMap[java.util.concurrent.Future[_], ActorRef]()

    val processingPromise =
      context.system.scheduler.schedule(0.seconds, 100.millis, self, Check)

    override def receive = {
      case task: java.util.concurrent.Future[_] =>
        tasks.put(task, sender())

      case Check =>
        tasks.foreach { case (task, sender) =>
          if (task.isDone) {
            if (dying && tasks.size % 2500 == 0) {
              log.info(s"Draining queue: ${tasks.size} messages")
            }
            sender ! task.get()
            tasks.remove(task)
          }
        }
        ()

      case Die =>
        dying = true
        log.info(s"Queue contains ${tasks.size} messages")
        val mySender = sender()
        if (tasks.isEmpty) {
          mySender ! Dead
          stopThyself()
        }
        else {
          context.system.scheduler.scheduleOnce(1.second, mySender, Die)
        }
        ()

      case message =>
        log.warning(s"Unhandled message '$message'")
        unhandled(message)
    }

    private def stopThyself() = {
      processingPromise.cancel()
      context.stop(self)
    }
  }

  case object Check

  case object Die

  case object Dead

}