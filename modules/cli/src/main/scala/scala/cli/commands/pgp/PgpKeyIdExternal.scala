package scala.cli.commands.pgp

import scala.cli.signing.commands.PgpKeyIdOptions

class PgpKeyIdExternal extends PgpExternalCommand {
  override def hidden = true
  def actualHelp      = PgpKeyIdOptions.help
  def externalCommand = Seq("pgp", "key-id")

  override def names = PgpCommandNames.pgpKeyId
}
