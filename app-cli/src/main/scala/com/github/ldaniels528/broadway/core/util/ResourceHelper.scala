package com.github.ldaniels528.broadway.core.util

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
  * Resource Helper Utility Class
  * @author lawrence.daniels@gmail.com
  */
object ResourceHelper {

  /**
    * Automatically closes a resource after the completion of a code block
    */
  implicit class AutoClose[T <: {def close()}](val resource: T) extends AnyVal {

    def use[S](block: T => S): S = try block(resource) finally resource.close()

  }

  /**
    * Automatically closes a resource after the completion of a code block
    */
  implicit class AutoDisconnect[T <: {def disconnect()}](val resource: T) extends AnyVal {

    def use[S](block: T => S): S = try block(resource) finally resource.disconnect()

  }

  /**
    * Automatically closes a resource after the completion of a code block
    */
  implicit class AutoShutdown[T <: {def shutdown()}](val resource: T) extends AnyVal {

    def use[S](block: T => S): S = try block(resource) finally resource.shutdown()

  }

  /**
    * Type require utilities
    * @param entity a given entity
    * @tparam A the entities type
    */
  implicit class TypeEnrichment[A](val entity: A) extends AnyVal {

    def require[B <: A](message: String)(implicit tag: ClassTag[B]) = entity match {
      case entityB: B => entityB
      case _ =>
        throw new IllegalArgumentException(message)
    }

  }

}
