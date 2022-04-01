package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.commands.ScalaCommand
import scala.cli.signing.commands.{PgpSign => OriginalPgpSign, PgpSignOptions}

object PgpSign extends ScalaCommand[PgpSignOptions] {

  override def inSipScala = false
  override def hidden     = true
  override def names      = PgpCommandNames.pgpSign

  def run(options: PgpSignOptions, args: RemainingArgs): Unit =
    OriginalPgpSign.run(options, args)
}
