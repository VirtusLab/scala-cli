package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions, SharedCompilationServerOptions, SharedDirectoriesOptions}

// format: off
final case class BloopOutputOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions()
) extends HasLoggingOptions
// format: on

object BloopOutputOptions {
  implicit lazy val parser: Parser[BloopOutputOptions] = Parser.derive
  implicit lazy val help: Help[BloopOutputOptions]   = Help.derive
}
