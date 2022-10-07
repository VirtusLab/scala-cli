package scala.cli.commands.pgp

import caseapp.core.RemainingArgs

import scala.cli.commands.ScalaCommand
import scala.cli.signing.commands.{PgpCreate => OriginalPgpCreate, PgpCreateOptions}

object PgpCreate extends ScalaCommand[PgpCreateOptions] {

  override def isRestricted = true
  override def hidden       = true
  override def names        = PgpCommandNames.pgpCreate

  override def runCommand(options: PgpCreateOptions, args: RemainingArgs): Unit =
    OriginalPgpCreate.run(options, args)
}
