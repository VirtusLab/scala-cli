package scala.cli.signing.commands

import caseapp._

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

// format: off
@HelpMessage("Sign files with PGP")
final case class PgpSignOptions(
  password: PasswordOption,
  secretKey: PasswordOption,
  @ExtraName("f")
    force: Boolean = false,
  stdout: Boolean = false
)
// format: on

object PgpSignOptions {
  implicit lazy val parser: Parser[PgpSignOptions] = Parser.derive
  implicit lazy val help: Help[PgpSignOptions]     = Help.derive
}
