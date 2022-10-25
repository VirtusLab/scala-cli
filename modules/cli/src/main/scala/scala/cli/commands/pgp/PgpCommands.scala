package scala.cli.commands.pgp

import caseapp.core.app.Command

class PgpCommands {
  def allScalaCommands: Array[PgpCommand[_]] =
    Array(PgpCreate, PgpKeyId, PgpSign, PgpVerify)
  def allExternalCommands: Array[ExternalCommand] =
    Array.empty
}
