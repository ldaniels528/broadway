package com.github.ldaniels528.broadway.cli.command

import com.github.ldaniels528.broadway.cli.command.params.CommandParameters

import scala.language.existentials

/**
  * Represents a Shell command
  * @author lawrence.daniels@gmail.com
  */
case class Command(name: String,
                   fx: UnixLikeArgs => Any,
                   params: CommandParameters[_],
                   help: String = "",
                   promptAware: Boolean = false,
                   undocumented: Boolean = false) {

  /**
    * Returns a usage prototype for this command
    * @return a usage prototype for this command (e.g. "zget [-t type] key")
    */
  def prototype: String = params.prototypeOf(this)

  /**
    * Returns the string representation for this command
    * @return a usage prototype for this command (e.g. "zget [-t type] key")
    */
  override def toString = prototype

}

