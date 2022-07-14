package scala.cli.commands.pgp

import coursier.cache.{ArchiveCache, Cache}
import coursier.util.Task
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.{Constants, FetchExternalBinary, Runner}
import scala.cli.ScalaCli
import scala.cli.commands.util.CommonOps._
import scala.util.Properties
import scala.build.internal.ExternalBinaryParams
import scala.build.internal.ExternalBinary
import scala.cli.commands.util.JvmUtils

abstract class PgpExternalCommand extends ExternalCommand {
  def progName: String = ScalaCli.progName
  def externalCommand: Seq[String]

  def tryRun(
    cache: Cache[Task],
    versionOpt: Option[String],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    allowExecve: Boolean,
    javaCommand: () => String
  ): Either[BuildException, Int] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(archiveCache, versionOpt, logger, javaCommand))

    val command = binary.command ++ externalCommand ++ args

    Runner.run0(
      progName,
      command,
      logger,
      allowExecve = allowExecve,
      cwd = None,
      extraEnv = extraEnv
    ).waitFor()
  }

  def output(
    cache: Cache[Task],
    versionOpt: Option[String],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    javaCommand: () => String
  ): Either[BuildException, String] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(archiveCache, versionOpt, logger, javaCommand))

    val command = binary.command ++ externalCommand ++ args

    os.proc(command).call(stdin = os.Inherit, env = extraEnv)
      .out.text()
  }

  def run(args: Seq[String]): Unit = {

    val (options, remainingArgs) =
      PgpExternalOptions.parser.stopAtFirstUnrecognized.parse(args) match {
        case Left(err) =>
          System.err.println(err.message)
          sys.exit(1)
        case Right((options0, remainingArgs0)) => (options0, remainingArgs0)
      }

    val logger = options.logging.logger

    val cache = options.coursier.coursierCache(logger.coursierLogger(""))
    val retCode = tryRun(
      cache,
      options.signingCliVersion.map(_.trim).filter(_.nonEmpty),
      remainingArgs,
      Map(),
      logger,
      allowExecve = true,
      () =>
        JvmUtils.javaOptions(options.jvm).javaHome(
          ArchiveCache().withCache(cache),
          cache,
          logger.verbosity
        ).value.javaCommand
    ).orExit(logger)

    if (retCode != 0)
      sys.exit(retCode)
  }
}

object PgpExternalCommand {
  def launcher(
    archiveCache: ArchiveCache[Task],
    versionOpt: Option[String],
    logger: Logger,
    javaCommand: () => String
  ): Either[BuildException, ExternalBinary] = {

    val platformSuffix = FetchExternalBinary.platformSuffix()
    val version = versionOpt
      .getOrElse(Constants.scalaCliSigningVersion)
    val (tag, changing) =
      if (version == "latest") ("launchers", true)
      else ("v" + version, false)
    val ext = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/scala-cli/scala-cli-signing/releases/download/$tag/scala-cli-signing-$platformSuffix$ext"

    val ver = if (version.startsWith("latest")) "latest.release" else version
    val params = ExternalBinaryParams(
      url,
      changing,
      "scala-cli-signing",
      Seq(
        dep"${Constants.scalaCliSigningOrganization}:${Constants.scalaCliSigningName}:$ver"
      ),
      "scala.cli.signing.ScalaCliSigning"
    )

    FetchExternalBinary.fetch(params, archiveCache, logger, javaCommand)
  }
}
