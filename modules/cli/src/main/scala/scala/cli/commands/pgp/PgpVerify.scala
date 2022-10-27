package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import caseapp.core.app.Command

import scala.cli.signing.commands.{PgpVerify => OriginalPgpVerify, PgpVerifyOptions}

object PgpVerify extends PgpCommand[PgpVerifyOptions] {
  override def names = PgpCommandNames.pgpVerify

  override def run(options: PgpVerifyOptions, args: RemainingArgs): Unit =
    OriginalPgpVerify.run(options, args)
}
