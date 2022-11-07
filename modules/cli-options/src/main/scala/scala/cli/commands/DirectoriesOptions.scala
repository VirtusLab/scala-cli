package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Prints directories used by Scala CLI")
final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions()
) extends HasLoggingOptions
// format: on

object DirectoriesOptions {
  implicit lazy val parser: Parser[DirectoriesOptions] = Parser.derive
  implicit lazy val help: Help[DirectoriesOptions]     = Help.derive
}
