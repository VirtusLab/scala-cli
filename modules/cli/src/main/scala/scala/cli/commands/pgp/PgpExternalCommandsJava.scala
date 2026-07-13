package scala.cli.commands.pgp

object PgpExternalCommandsJava {
  def all(): Array[ExternalCommand] =
    Array(
      new PgpCreateExternal(),
      new PgpKeyIdExternal(),
      new PgpSignExternal(),
      new PgpVerifyExternal()
    )
}
