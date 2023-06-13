package scala.cli.commands.pgp

/** Used for choosing the right PGP proxy implementation when Scala CLI is run on JVM. <br>
  *
  * See [[scala.cli.internal.PgpProxyMakerSubst PgpProxyMakerSubst]]
  */
class PgpProxyMaker {
  def get(forceSigningExternally: java.lang.Boolean): PgpProxy =
    if (forceSigningExternally)
      new PgpProxy
    else
      new PgpProxyJvm
}
