package scala.cli.commands

import caseapp.*

// format: off
final case class CrossOptions(
  @Tag(tags.experimental)
  @HelpMessage("Run given command against all provided Scala versions and/or platforms")
    cross: Option[Boolean] = None
)
// format: on

object CrossOptions {
  implicit lazy val parser: Parser[CrossOptions] = Parser.derive
  implicit lazy val help: Help[CrossOptions]     = Help.derive
}
