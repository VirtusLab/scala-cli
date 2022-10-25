package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Print `scala-cli` version")
final case class VersionOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @HelpMessage("Show only plain scala-cli version")
  @Name("cli")
    cliVersion: Boolean = false,
  @HelpMessage("Show only plain scala version") 
  @Name("scala")
    scalaVersion: Boolean = false
) extends HasLoggingOptions
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive
}
