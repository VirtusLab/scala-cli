package scala.cli.commands.pgp

import coursier.cache.Cache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.errors.PgpError
import scala.cli.signing.commands.PgpKeyId

class PgpProxyJvm extends PgpProxy {
  override def keyId(
    key: String,
    keyPrintablePath: String,
    cache: Cache[Task],
    logger: Logger
  ): Either[BuildException, String] =
    PgpKeyId.get(key.getBytes(StandardCharsets.UTF_8), fingerprint = false)
      .headOption
      .toRight {
        new PgpError(s"No public key found in $keyPrintablePath")
      }
}
