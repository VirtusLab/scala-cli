package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasLoggingOptions

// format: off
@HelpMessage("Prints directories used by `scala-cli`")
final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions()
) extends HasLoggingOptions
// format: on

object DirectoriesOptions {
  lazy val parser: Parser[DirectoriesOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[DirectoriesOptions, parser.D] = parser
  implicit lazy val help: Help[DirectoriesOptions]                      = Help.derive
}
