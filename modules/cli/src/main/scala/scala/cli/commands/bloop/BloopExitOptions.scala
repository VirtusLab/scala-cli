package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{CoursierOptions, HasLoggingOptions, LoggingOptions, SharedCompilationServerOptions, SharedDirectoriesOptions}


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
}
