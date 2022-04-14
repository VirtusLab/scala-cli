package scala.cli.commands.pgp

import scala.cli.signing.commands.PgpSignOptions

class PgpSignExternal extends PgpExternalCommand {
  override def hidden = true
  def actualHelp      = PgpSignOptions.help
  def externalCommand = Seq("pgp", "sign")

  override def names = PgpCommandNames.pgpSign
}
