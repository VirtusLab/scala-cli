package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.commands.ScalaCommand
import scala.cli.signing.commands.{PgpVerify => OriginalPgpVerify, PgpVerifyOptions}

object PgpVerify extends ScalaCommand[PgpVerifyOptions] {

  override def inSipScala = false
  override def hidden     = true
  override def names      = PgpCommandNames.pgpVerify

  def run(options: PgpVerifyOptions, args: RemainingArgs): Unit =
    OriginalPgpVerify.run(options, args)
}
