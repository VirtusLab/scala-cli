package scala.cli.commands

import caseapp._

// format: off
final case class CompileCrossOptions(
  @HelpMessage("Cross-compile sources")
    cross: Option[Boolean] = None
)
// format: on

object CompileCrossOptions {
  implicit lazy val parser                          = Parser[CompileCrossOptions]
  implicit lazy val help: Help[CompileCrossOptions] = Help.derive
}
