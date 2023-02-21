package scala.cli.commands.uninstallcompletions

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasLoggingOptions, HelpMessages, LoggingOptions}
import scala.cli.commands.uninstallcompletions.SharedUninstallCompletionsOptions

// format: off
@HelpMessage(UninstallCompletionsOptions.helpMessage, "", UninstallCompletionsOptions.detailedHelpMessage)
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

  private val helpHeader = s"Uninstalls $fullRunnerName completions from your shell."
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference("uninstall completions")}
       |${HelpMessages.commandDocWebsiteReference("completions")}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandDocWebsiteReference("completions")}""".stripMargin
}
