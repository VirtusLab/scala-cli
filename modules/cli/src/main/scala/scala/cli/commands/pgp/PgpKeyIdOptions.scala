package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup}
import scala.cli.commands.tags

// format: off
@HelpMessage("Print the key ID of PGP public keys")
final case class PgpKeyIdOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Print the key fingerprint rather than the key ID")
  @Tag(tags.experimental)
    fingerprint: Boolean = false
) extends HasGlobalOptions
// format: on

object PgpKeyIdOptions {
  implicit lazy val parser: Parser[PgpKeyIdOptions] = Parser.derive
  implicit lazy val help: Help[PgpKeyIdOptions]     = Help.derive
}
