package com.github.ldaniels528.broadway.core.io.filters.impl

import com.github.ldaniels528.broadway.core.io.Scope
import com.github.ldaniels528.broadway.core.io.filters.Filter

/**
  * Trim Filter
  * @author lawrence.daniels@gmail.com
  */
case class TrimFilter() extends Filter {

  override def execute(value: Option[Any], args: List[String])(implicit scope: Scope) = {
    value map {
      case s: String => s.trim
      case v => v
    }
  }

}
