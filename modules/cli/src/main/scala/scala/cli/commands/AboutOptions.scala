package scala.cli.commands

import caseapp._

@HelpMessage("Print details about this application")
final case class AboutOptions()

object AboutOptions {
  implicit lazy val parser: Parser[AboutOptions] = Parser.derive
  implicit lazy val help: Help[AboutOptions]   = Help.derive
}
