package scala.cli.commands.uninstallcompletions

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, HelpMessages, LoggingOptions}
import scala.cli.commands.uninstallcompletions.SharedUninstallCompletionsOptions

// format: off
@HelpMessage(
  s"""Uninstalls completions from your shell
     |
     |${HelpMessages.commandDocWebsiteReference("completions")}""".stripMargin)
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
