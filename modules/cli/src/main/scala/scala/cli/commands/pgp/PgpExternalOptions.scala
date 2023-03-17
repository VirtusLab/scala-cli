package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{
  CoursierOptions,
  GlobalOptions,
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  LoggingOptions,
  SharedJvmOptions
}

// format: off
final case class PgpExternalOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions()
) extends HasGlobalOptions
// format: on

object PgpExternalOptions {
  implicit lazy val parser: Parser[PgpExternalOptions] = Parser.derive
  implicit lazy val help: Help[PgpExternalOptions]     = Help.derive
}
