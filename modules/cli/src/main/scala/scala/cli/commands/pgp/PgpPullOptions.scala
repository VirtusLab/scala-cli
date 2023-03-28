package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{
  GlobalOptions,
  GlobalSuppressWarningOptions,
  HasGlobalOptions,
  HelpGroup,
  LoggingOptions
}

// format: off
final case class PgpPullOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Recurse
    shared: SharedPgpPushPullOptions = SharedPgpPushPullOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Whether to exit with code 0 if no key is passed")
    allowEmpty: Boolean = false
) extends HasGlobalOptions
// format: on

object PgpPullOptions {
  implicit lazy val parser: Parser[PgpPullOptions] = Parser.derive
  implicit lazy val help: Help[PgpPullOptions]     = Help.derive
}
