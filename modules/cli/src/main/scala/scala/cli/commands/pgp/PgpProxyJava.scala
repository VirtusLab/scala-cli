package scala.cli.commands.pgp

object PgpProxyJava {
  def create(): PgpProxy = new PgpNativeProxy
}
