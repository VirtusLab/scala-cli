package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.signing.commands.{PgpSign as OriginalPgpSign, PgpSignOptions}
import scala.cli.signing.util.BouncycastleSetup

object PgpSign extends PgpCommand[PgpSignOptions] {
  override def names = PgpCommandNames.pgpSign

  override def run(options: PgpSignOptions, args: RemainingArgs): Unit = {
    BouncycastleSetup.ensureProviderRegistered()
    OriginalPgpSign.run(options, args)
  }
}
