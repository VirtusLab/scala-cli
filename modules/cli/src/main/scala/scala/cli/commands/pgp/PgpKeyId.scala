package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.commands.ScalaCommand
import scala.cli.signing.commands.{PgpKeyId => OriginalPgpKeyId, PgpKeyIdOptions}

object PgpKeyId extends ScalaCommand[PgpKeyIdOptions] {

  override def isRestricted = true
  override def hidden       = true
  override def names        = PgpCommandNames.pgpKeyId

  def run(options: PgpKeyIdOptions, args: RemainingArgs): Unit =
    OriginalPgpKeyId.run(options, args)
}
