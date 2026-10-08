package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup}
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage("Sign files with PGP")
final case class PgpSignOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Password of the secret key, e.g. value:… / env:… / file:… / command:…")
  @ValueDescription("password")
  @Tag(tags.experimental)
    password: Option[PasswordOption] = None,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Secret key to sign files with, e.g. file:… / env:… / value:… (can be base64-encoded)")
  @ValueDescription("key")
  @Tag(tags.experimental)
  @Tag(tags.inShortHelp)
    secretKey: PasswordOption,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Overwrite already existing signature files")
  @ExtraName("f")
  @Tag(tags.experimental)
    force: Boolean = false,
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Print the signature to the standard output, rather than writing it to a .asc file (only allowed with a single input file)")
  @Tag(tags.experimental)
    stdout: Boolean = false
) extends HasGlobalOptions
// format: on

object PgpSignOptions {
  implicit lazy val parser: Parser[PgpSignOptions] = Parser.derive
  implicit lazy val help: Help[PgpSignOptions]     = Help.derive
}
