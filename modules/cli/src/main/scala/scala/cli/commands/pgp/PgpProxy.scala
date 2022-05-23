package scala.cli.commands.pgp

import coursier.cache.Cache
import coursier.util.Task

import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.pgp.PgpKeyIdExternal
import scala.cli.errors.PgpError
import scala.util.Properties

class PgpProxy {
  def keyId(
    key: String,
    keyPrintablePath: String,
    cache: Cache[Task],
    logger: Logger
  ): Either[BuildException, String] = {
    val keyPath =
      if (Properties.isWin)
        os.temp(key, prefix = "key", suffix = ".pub")
      else
        os.temp(key, prefix = "key", suffix = ".pub", perms = "rwx------")
    val maybeRawOutput =
      try {
        (new PgpKeyIdExternal).output(
          cache,
          None,
          Seq(keyPath.toString),
          Map(),
          logger
        ).map(_.trim)
      }
      finally os.remove(keyPath)
    maybeRawOutput.flatMap { rawOutput =>
      if (rawOutput.isEmpty)
        Left(new PgpError(s"No public key found in $keyPrintablePath"))
      else
        Right(rawOutput)
    }
  }
}
