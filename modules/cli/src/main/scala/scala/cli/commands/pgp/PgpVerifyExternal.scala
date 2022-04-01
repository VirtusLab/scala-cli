package scala.cli.commands.pgp

import scala.cli.signing.commands.PgpVerifyOptions

class PgpVerifyExternal extends PgpExternalCommand {
  def actualHelp      = PgpVerifyOptions.help
  def externalCommand = Seq("pgp", "verify")

  override def names = PgpCommandNames.pgpVerify
}
