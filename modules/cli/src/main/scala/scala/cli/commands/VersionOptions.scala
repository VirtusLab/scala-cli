package scala.cli.commands

import caseapp._

@HelpMessage("Print `scala-cli` version")
final case class VersionOptions()

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]         = Help.derive
}
