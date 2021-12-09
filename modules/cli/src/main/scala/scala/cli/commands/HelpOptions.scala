package scala.cli.commands

import caseapp._

@HelpMessage("Print help message")
case class HelpOptions()

object HelpOptions {
  implicit lazy val parser: Parser[HelpOptions] = Parser.derive
  implicit lazy val help: Help[HelpOptions]     = Help.derive
}
