package scala.cli.commands.pgp

import coursier.cache.Cache
import coursier.util.Task

import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.pgp.{PgpCreateExternal, PgpKeyIdExternal}
import scala.cli.errors.PgpError
import scala.util.Properties

class PgpProxy {
  def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    password: String,
    cache: Cache[Task],
    logger: Logger
  ): Either[BuildException, Int] = {
    val quietOptions = Nil
    (new PgpCreateExternal).tryRun(
      cache,
      None,
      Seq(
        "pgp",
        "create",
        "--pub-dest",
        pubKey.toString,
        "--secret-dest",
        secKey.toString,
        "--email",
        mail,
        "--password",
        s"env:SCALA_CLI_RANDOM_KEY_PASSWORD"
      ) ++ quietOptions,
      Map("SCALA_CLI_RANDOM_KEY_PASSWORD" -> password),
      logger,
      allowExecve = false
    )
  }

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
