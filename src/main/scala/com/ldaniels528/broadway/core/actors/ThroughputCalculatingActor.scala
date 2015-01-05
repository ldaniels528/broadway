package com.ldaniels528.broadway.core.actors

import akka.actor.Actor
import com.ldaniels528.broadway.core.actors.Actors.BWxActorRef
import com.ldaniels528.broadway.core.actors.Actors.Implicits._
import com.ldaniels528.broadway.core.util.ThroughputCalculator

/**
 * Throughput Calculating Actor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ThroughputCalculatingActor(label: String, host: BWxActorRef) extends Actor {
  private val calculator = new ThroughputCalculator(label)

  override def receive = {
    case message =>
      host ! message
      calculator.update(1)
  }

}