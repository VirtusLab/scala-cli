package scala.cli.commands.pgp

import scala.cli.signing.commands.PgpVerifyOptions

class PgpVerifyExternal extends PgpExternalCommand {
  override def hidden = true
  def actualHelp      = PgpVerifyOptions.help
  def externalCommand = Seq("pgp", "verify")

  override def names = PgpCommandNames.pgpVerify
}
