package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{CoursierOptions, HasLoggingOptions, HelpMessages, LoggingOptions, SharedCompilationServerOptions, SharedJvmOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Starts a Bloop instance, if none is running.
     |
     |${HelpMessages.bloopInfo}""".stripMargin)
final case class BloopStartOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Name("f")
  @Tag(tags.restricted)
    force: Boolean = false
) extends HasLoggingOptions
// format: on

object BloopStartOptions {
  implicit lazy val parser: Parser[BloopStartOptions] = Parser.derive
  implicit lazy val help: Help[BloopStartOptions]     = Help.derive
}
