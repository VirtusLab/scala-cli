package scala.cli.commands.version

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Print version")
final case class VersionOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Tag(tags.implementation)
  @HelpMessage("Show only plain version")
  @Name("cli")
    cliVersion: Boolean = false,
  @HelpMessage("Show only plain scala version")
  @Tag(tags.implementation)
  @Name("scala")
    scalaVersion: Boolean = false
) extends HasLoggingOptions
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive
}
