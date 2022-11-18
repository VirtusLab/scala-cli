package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{
  CoursierOptions,
  HasLoggingOptions,
  LoggingOptions,
  SharedJvmOptions
}

// format: off
final case class PgpPushOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    shared: SharedPgpPushPullOptions = SharedPgpPushPullOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),

  @Group("PGP")
  @HelpMessage("Try to push the key even if Scala CLI thinks it's not a public key")
  @ExtraName("f")
    force: Boolean = false,
  @Group("PGP")
  @HelpMessage("Whether to exit with code 0 if no key is passed")
    allowEmpty: Boolean = false,
  @Group("PGP")
  @Hidden
    forceSigningBinary: Boolean = false
) extends HasLoggingOptions
// format: on

object PgpPushOptions {
  implicit lazy val parser: Parser[PgpPushOptions] = Parser.derive
  implicit lazy val help: Help[PgpPushOptions]     = Help.derive
}
