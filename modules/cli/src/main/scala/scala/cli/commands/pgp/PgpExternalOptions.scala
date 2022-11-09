package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{
  CoursierOptions,
  HasLoggingOptions,
  LoggingOptions,
  SharedJvmOptions
}

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
