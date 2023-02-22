package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, HelpGroup, LoggingOptions}

// format: off
final case class PgpPullOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    shared: SharedPgpPushPullOptions = SharedPgpPushPullOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Whether to exit with code 0 if no key is passed")
    allowEmpty: Boolean = false
) extends HasLoggingOptions
// format: on

object PgpPullOptions {
  implicit lazy val parser: Parser[PgpPullOptions] = Parser.derive
  implicit lazy val help: Help[PgpPullOptions]     = Help.derive
}
