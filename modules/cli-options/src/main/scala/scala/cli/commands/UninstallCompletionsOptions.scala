package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Uninstalls completions from your shell")
final case class UninstallCompletionsOptions(
  @Recurse
    shared: SharedUninstallCompletionsOptions = SharedUninstallCompletionsOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions()
) extends HasLoggingOptions
// format: on

object UninstallCompletionsOptions {
  implicit lazy val parser: Parser[UninstallCompletionsOptions] = Parser.derive
  implicit lazy val help: Help[UninstallCompletionsOptions]     = Help.derive
}
