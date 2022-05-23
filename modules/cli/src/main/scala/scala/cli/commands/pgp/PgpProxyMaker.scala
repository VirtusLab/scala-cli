package scala.cli.commands.pgp

class PgpProxyMaker {
  def get(): PgpProxy =
    new PgpProxyJvm
}
