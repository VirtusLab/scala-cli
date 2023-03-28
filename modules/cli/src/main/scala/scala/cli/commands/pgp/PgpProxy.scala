package scala.cli.commands.pgp

import coursier.cache.{Cache, FileCache}
import coursier.util.Task

import scala.build.errors.BuildException
import scala.build.{Logger, options => bo}
import scala.cli.commands.pgp.{PgpCreateExternal, PgpKeyIdExternal}
import scala.cli.errors.PgpError
import scala.util.Properties

class PgpProxy {
  def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String],
    cache: FileCache[Task],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Int] = {

    val (passwordOption, extraEnv) = passwordOpt match
      case Some(value) =>
        (
          Seq("--password", s"env:SCALA_CLI_RANDOM_KEY_PASSWORD"),
          Map("SCALA_CLI_RANDOM_KEY_PASSWORD" -> value)
        )
      case None => (Nil, Map.empty)
    val quietOptions = Nil
    (new PgpCreateExternal).tryRun(
      cache,
      Seq(
        "pgp",
        "create",
        "--pub-dest",
        pubKey,
        "--secret-dest",
        secKey,
        "--email",
        mail
      ) ++ passwordOption ++ quietOptions,
      extraEnv,
      logger,
      allowExecve = false,
      javaCommand,
      signingCliOptions
    )
  }

  def keyId(
    key: String,
    keyPrintablePath: String,
    cache: FileCache[Task],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
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
          Seq(keyPath.toString),
          Map(),
          logger,
          javaCommand,
          signingCliOptions
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
