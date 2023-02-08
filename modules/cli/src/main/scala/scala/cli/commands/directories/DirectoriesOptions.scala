package scala.cli.commands.directories

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}

// format: off
@HelpMessage("Prints directories used by Scala CLI")
final case class DirectoriesOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions()
) extends HasLoggingOptions
// format: on

object DirectoriesOptions {
  implicit lazy val parser: Parser[DirectoriesOptions] = Parser.derive
  implicit lazy val help: Help[DirectoriesOptions]     = Help.derive
}
