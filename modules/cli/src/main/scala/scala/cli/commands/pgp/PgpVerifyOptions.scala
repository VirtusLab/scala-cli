package scala.cli.commands.pgp

import caseapp.*

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup}
import scala.cli.commands.tags

// format: off
@HelpMessage("Verify PGP signatures")
final case class PgpVerifyOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Group(HelpGroup.PGP.toString)
  @HelpMessage("Path to the public key to verify signatures with")
  @ValueDescription("path")
  @Tag(tags.experimental)
  @Tag(tags.inShortHelp)
    key: String
) extends HasGlobalOptions {
  // format: on
  def keyPath: os.Path = os.Path(key, os.pwd)
}

object PgpVerifyOptions {
  implicit lazy val parser: Parser[PgpVerifyOptions] = Parser.derive
  implicit lazy val help: Help[PgpVerifyOptions]     = Help.derive
}
