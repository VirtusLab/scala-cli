package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Print `scala-cli` version")
final case class VersionOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions()
)
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]         = Help.derive
}
