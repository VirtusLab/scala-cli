package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasLoggingOptions


// format: off
final case class BloopExitOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions()
) extends HasLoggingOptions
// format: on

object BloopExitOptions {
  implicit lazy val parser: Parser[BloopExitOptions] = Parser.derive
  implicit lazy val help: Help[BloopExitOptions]     = Help.derive
  // Parser.Aux for using BloopExitOptions with @Recurse in other options
  implicit lazy val parserAux: Parser.Aux[BloopExitOptions, parser.D] = parser
}
