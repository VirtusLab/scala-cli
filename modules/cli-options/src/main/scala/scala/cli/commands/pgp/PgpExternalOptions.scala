package scala.cli.commands.pgp

import caseapp._

import scala.cli.commands.{CoursierOptions, LoggingOptions}
import scala.cli.commands.SharedJvmOptions
import scala.cli.commands.common.HasLoggingOptions

// format: off
final case class PgpExternalOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Hidden
    signingCliVersion: Option[String] = None
) extends HasLoggingOptions
// format: on

object PgpExternalOptions {
  implicit lazy val parser: Parser[PgpExternalOptions] = Parser.derive
  implicit lazy val help: Help[PgpExternalOptions]     = Help.derive
}
