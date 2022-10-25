package scala.cli.commands.bloop

import caseapp._

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.commands.{CoursierOptions, LoggingOptions, SharedCompilationServerOptions, SharedDirectoriesOptions, SharedJvmOptions}

// format: off
final case class BloopOptions(
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

  @ExtraName("workingDir")
  @ExtraName("dir")
    workingDirectory: Option[String] = None
) extends HasLoggingOptions {
  // format: on

  def workDirOpt: Option[os.Path] =
    workingDirectory
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))
}

object BloopOptions {
  implicit lazy val parser: Parser[BloopOptions] = Parser.derive
  implicit lazy val help: Help[BloopOptions]   = Help.derive
}
