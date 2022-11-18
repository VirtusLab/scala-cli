package scala.cli.commands

import caseapp.*
import caseapp.core.help.RuntimeCommandsHelp

import scala.build.Logger
import scala.cli.ScalaCliHelp

class HelpCmd(actualHelp: => RuntimeCommandsHelp) extends ScalaCommand[HelpOptions] {
  override def names                   = List(List("help"))
  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(options: HelpOptions, args: RemainingArgs, logger: Logger): Unit =
    println(actualHelp.help(ScalaCliHelp.helpFormat))
}
