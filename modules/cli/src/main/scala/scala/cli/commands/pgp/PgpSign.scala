package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import caseapp.core.app.Command

import scala.cli.signing.commands.{PgpSign => OriginalPgpSign, PgpSignOptions}

object PgpSign extends PgpCommand[PgpSignOptions] {
  override def names = PgpCommandNames.pgpSign

  override def run(options: PgpSignOptions, args: RemainingArgs): Unit =
    OriginalPgpSign.run(options, args)
}
