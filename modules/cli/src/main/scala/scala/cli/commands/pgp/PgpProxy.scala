package scala.cli.commands.pgp

import coursier.cache.{Cache, FileCache}
import coursier.util.Task

import scala.build.errors.BuildException
import scala.build.{Logger, options as bo}
import scala.cli.commands.pgp.{PgpCreateExternal, PgpKeyIdExternal}
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions}
import scala.cli.errors.PgpError
import scala.util.Properties

/** A proxy running the PGP operations externally using scala-cli-singing. This is done either using
  * it's native image launchers or running it in a JVM process. This construct is not used when PGP
  * commands are evoked from CLI (see [[PgpCommandsSubst]] and [[PgpCommands]]), but rather when PGP
  * operations are used internally. <br>
  *
  * This is the 'native' counterpart of [[PgpProxyJvm]]
  */
class PgpProxy {
  def createKey(
    pubKey: String,
    secKey: String,
    mail: String,
    quiet: Boolean,
    passwordOpt: Option[String],
    cache: FileCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
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
      jvmOptions,
      coursierOptions,
      signingCliOptions
    )
  }

  def keyId(
    key: String,
    keyPrintablePath: String,
    cache: FileCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
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
          jvmOptions,
          coursierOptions,
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
