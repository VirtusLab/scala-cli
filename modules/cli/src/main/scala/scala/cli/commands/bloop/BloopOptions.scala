package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{CoursierOptions, HasLoggingOptions, HelpMessages, LoggingOptions, SharedCompilationServerOptions, SharedJvmOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Interact with Bloop (the build server) or check its status.
     |
     |This sub-command allows to check the current status of Bloop.
     |If Bloop isn't currently running, it will be started.
     |
     |${HelpMessages.bloopInfo}""".stripMargin)
final case class BloopOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),

  @ExtraName("workingDir")
  @ExtraName("dir")
  @Tag(tags.restricted)
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
