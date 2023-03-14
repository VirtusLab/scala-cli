package scala.cli.commands.clean

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  HelpMessages,
  LoggingOptions,
  SharedBspFileOptions,
  SharedWorkspaceOptions
}

// format: off
@HelpMessage(CleanOptions.helpMessage, "", CleanOptions.detailedHelpMessage)
final case class CleanOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    globalSuppressWarning: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions(),
  @Recurse
    bspFile: SharedBspFileOptions = SharedBspFileOptions(),
  @Recurse
    workspace: SharedWorkspaceOptions = SharedWorkspaceOptions()
) extends HasGlobalOptions
// format: on

object CleanOptions {
  implicit lazy val parser: Parser[CleanOptions] = Parser.derive
  implicit lazy val help: Help[CleanOptions]     = Help.derive
  val cmdName                                    = "clean"
  private val helpHeader                         = "Clean the workspace."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |Passed inputs will establish the $fullRunnerName project, for which the workspace will be cleaned.
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
