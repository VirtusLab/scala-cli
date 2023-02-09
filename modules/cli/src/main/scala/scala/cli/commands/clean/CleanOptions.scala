package scala.cli.commands.clean

import caseapp.*

import scala.cli.commands.shared.{
  HasLoggingOptions,
  LoggingOptions,
  SharedBspFileOptions,
  SharedWorkspaceOptions
}

// format: off
@HelpMessage("Clean the workspace")
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
