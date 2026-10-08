package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.signing.commands.{PgpKeyId as OriginalPgpKeyId, PgpKeyIdOptions}
import scala.cli.signing.util.BouncycastleSetup

object PgpKeyId extends PgpCommand[PgpKeyIdOptions] {
  override def names: List[List[String]] = PgpCommandNames.pgpKeyId

  override def run(options: PgpKeyIdOptions, args: RemainingArgs): Unit = {
    BouncycastleSetup.ensureProviderRegistered()
    OriginalPgpKeyId.run(options, args)
  }
}
