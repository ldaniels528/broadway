package com.ldaniels528.broadway.core.util

import java.io._
import java.nio.file.{Paths, StandardWatchEventKinds}

import akka.actor.ActorSystem
import com.ldaniels528.broadway.core.util.FileMonitor.QueuedFile
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.{implicitConversions, postfixOps}
import scala.util.Try

/**
 * File Monitor
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class FileMonitor(system: ActorSystem) {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  private implicit val ec = system.dispatcher
  private val queue = TrieMap[File, QueuedFile]()

  // check for files once per second
  system.scheduler.schedule(initialDelay = 0 seconds, interval = 1 second) {
    Try {
      // check for completed files
      for (qf <- queue.values) {
        // is the file ready?
        if (qf.isReady) {
          queue.remove(qf.file)
          qf.callback(qf.file)
        }
      }
    }
    ()
  }

  /**
   * Listens for files in the specified directory, and processes them
   * with the given callback function.
   * @param directory the specified directory
   * @param callback the given callback function
   */
  @throws[IOException]
  def listenForFiles(directory: File)(callback: File => Unit) = {
    // create the watcher service, and register for new file events
    val path = Paths.get(directory.getAbsolutePath)
    val watcher = path.getFileSystem.newWatchService()
    path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE)
    logger.info(s"Watching for new files in '${directory.getAbsolutePath}'...")

    // retrieve the next watch key
    Future(watcher.take()) map { watchKey =>
      system.scheduler.schedule(initialDelay = 0 seconds, interval = 1 second) {
        Try {
          // poll for new events
          val events = watchKey.pollEvents()
          if (events.nonEmpty) {
            for (event <- events) {
              if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                // get a reference to the new file
                val file = new File(directory, event.context().toString)
                logger.info(s"Waiting to consume '${file.getName}' (${directory.getAbsolutePath})...")

                // register to be notified when the file is ready for consumption
                notifyWhenReady(file)(file => Future {
                  callback(file); ()
                })
              }
            }
          }
        }
        ()
      }
    }
  }

  /**
   * notifies the caller when the file is ready
   * @param file the given [[File]]
   * @param callback the given callback function
   */
  def notifyWhenReady(file: File)(callback: File => Unit) {
    queue.putIfAbsent(file, new QueuedFile(file, callback))
    ()
  }

}

/**
 * File Monitor Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object FileMonitor {

  /**
   * Represents a queued file
   * @author Lawrence Daniels <lawrence.daniels@gmail.com>
   */
  case class QueuedFile(file: File, callback: File => Unit) {
    // capture the file's initial state
    private var state0 = FileChangeState(file.lastModified(), file.length())

    /**
     * Attempts to determine whether the file is complete or not
     * @return true, if the file's size or last modified time hasn't changed in [up to] 10 seconds
     */
    def isReady: Boolean = {
      if (state0.elapsed < 1.second) false
      else {
        // get the last modified time and file size
        val state1 = FileChangeState(file.lastModified(), file.length())

        // has the file changed?
        val unchanged = state0.time == state1.time && state0.size == state1.size
        if (!unchanged) {
          state0 = state1.copy(lastChange = System.currentTimeMillis())
        }

        // return the result
        state0.elapsed >= 30.seconds && unchanged
      }
    }
  }

  case class FileChangeState(time: Long, size: Long, lastChange: Long = System.currentTimeMillis()) {
    def elapsed = System.currentTimeMillis() - lastChange
  }

  implicit def duration2Long(duration: Duration): Long = duration.toMillis

}