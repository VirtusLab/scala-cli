package scala.cli.commands.clean

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{
  HasLoggingOptions,
  HelpMessages,
  LoggingOptions,
  SharedBspFileOptions,
  SharedWorkspaceOptions
}

// format: off
@HelpMessage(
  s"""Clean the workspace.
    |
    |Passed inputs will establish the $fullRunnerName project, for which the workspace will be cleaned.
    |
    |${HelpMessages.docsWebsiteReference}""".stripMargin)
final case class CleanOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    bspFile: SharedBspFileOptions = SharedBspFileOptions(),
  @Recurse
    workspace: SharedWorkspaceOptions = SharedWorkspaceOptions()
) extends HasLoggingOptions
// format: on

object CleanOptions {
  implicit lazy val parser: Parser[CleanOptions] = Parser.derive
  implicit lazy val help: Help[CleanOptions]     = Help.derive
}
