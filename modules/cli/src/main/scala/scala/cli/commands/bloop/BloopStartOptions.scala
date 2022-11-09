package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{CoursierOptions, HasLoggingOptions, LoggingOptions, SharedCompilationServerOptions, SharedDirectoriesOptions, SharedJvmOptions}

// format: off
final case class BloopStartOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Name("f")
    force: Boolean = false
) extends HasLoggingOptions
// format: on

object BloopStartOptions {
  implicit lazy val parser: Parser[BloopStartOptions] = Parser.derive
  implicit lazy val help: Help[BloopStartOptions]     = Help.derive
}
