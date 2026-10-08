package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.signing.commands.{PgpVerify as OriginalPgpVerify, PgpVerifyOptions}
import scala.cli.signing.util.BouncycastleSetup

object PgpVerify extends PgpCommand[PgpVerifyOptions] {
  override def names = PgpCommandNames.pgpVerify

  override def run(options: PgpVerifyOptions, args: RemainingArgs): Unit = {
    BouncycastleSetup.ensureProviderRegistered()
    OriginalPgpVerify.run(options, args)
  }
}
