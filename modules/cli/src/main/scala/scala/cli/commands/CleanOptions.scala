package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Clean-up workspace")
final case class CleanOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions()
)
// format: on

object CleanOptions {
  implicit lazy val parser: Parser[CleanOptions] = Parser.derive
  implicit lazy val help: Help[CleanOptions]     = Help.derive
}
