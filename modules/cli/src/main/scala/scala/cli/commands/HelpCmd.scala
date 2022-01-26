package scala.cli.commands

import caseapp._
import caseapp.core.help.RuntimeCommandsHelp

import scala.cli.ScalaCliHelp

class HelpCmd(actualHelp: => RuntimeCommandsHelp) extends ScalaCommand[HelpOptions] {
  override def names = List(List("help"))

  def run(options: HelpOptions, args: RemainingArgs) =
    println(actualHelp.help(ScalaCliHelp.helpFormat))
}
