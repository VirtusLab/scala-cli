package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Prints directories used by `scala-cli`")
final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions()
)
// format: on

object DirectoriesOptions {
  lazy val parser: Parser[DirectoriesOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[DirectoriesOptions, parser.D] = parser
  implicit lazy val help: Help[DirectoriesOptions]                      = Help.derive
}
