package scala.cli.commands

import caseapp.*
import caseapp.core.help.RuntimeCommandsHelp

import scala.build.Logger
import scala.cli.commands.shared.{HelpOptions, ScalaCliHelp}

class HelpCmd(actualHelp: => RuntimeCommandsHelp)
    extends ScalaCommandWithCustomHelp[HelpOptions](actualHelp) {
  override def names                   = List(List("help"))
  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(options: HelpOptions, args: RemainingArgs, logger: Logger): Unit =
    customHelpAsked(showHidden = false)
}
