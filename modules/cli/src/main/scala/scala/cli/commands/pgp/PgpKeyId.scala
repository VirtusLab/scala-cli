package scala.cli.commands.pgp

import caseapp.core.RemainingArgs
import caseapp.core.app.Command

import scala.cli.signing.commands.{PgpKeyId => OriginalPgpKeyId, PgpKeyIdOptions}

object PgpKeyId extends PgpCommand[PgpKeyIdOptions] {
  override def names: List[List[String]] = PgpCommandNames.pgpKeyId

  override def run(options: PgpKeyIdOptions, args: RemainingArgs): Unit =
    OriginalPgpKeyId.run(options, args)
}
