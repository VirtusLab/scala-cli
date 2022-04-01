package scala.cli.commands.pgp

import scala.cli.signing.commands.PgpCreateOptions

class PgpCreateExternal extends PgpExternalCommand {
  def actualHelp      = PgpCreateOptions.help
  def externalCommand = Seq("pgp", "create")

  override def names = PgpCommandNames.pgpCreate
}
