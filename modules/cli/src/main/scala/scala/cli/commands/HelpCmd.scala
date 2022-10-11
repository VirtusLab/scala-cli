package scala.cli.commands

import caseapp.*
import caseapp.core.help.RuntimeCommandsHelp

import scala.cli.ScalaCliHelp

class HelpCmd(actualHelp: => RuntimeCommandsHelp) extends ScalaCommand[HelpOptions] {
  override def names = List(List("help"))

  override def runCommand(options: HelpOptions, args: RemainingArgs) =
    println(actualHelp.help(ScalaCliHelp.helpFormat))
}
