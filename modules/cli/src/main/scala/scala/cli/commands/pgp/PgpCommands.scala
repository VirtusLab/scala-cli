package scala.cli.commands.pgp

class PgpCommands {
  def allScalaCommands: Array[PgpCommand[?]] =
    Array(PgpCreate, PgpKeyId, PgpSign, PgpVerify)
  def allExternalCommands: Array[ExternalCommand] =
    Array.empty
}
