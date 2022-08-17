package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Print `scala-cli` version")
final case class VersionOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions(),
  @HelpMessage("Show only plain scala-cli version")
  @Name("cli")
    cliVersion: Boolean = false,
  @HelpMessage("Show only plain scala version") 
  @Name("scala")
    scalaVersion: Boolean = false
)
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive
}
