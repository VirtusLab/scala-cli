package scala.cli.commands.pgp

import coursier.cache.{ArchiveCache, Cache}
import coursier.util.Task

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.{Constants, FetchExternalBinary, Runner}
import scala.cli.ScalaCli
import scala.cli.commands.util.CommonOps._
import scala.util.Properties

abstract class PgpExternalCommand extends ExternalCommand {
  def progName: String = ScalaCli.progName
  def externalCommand: Seq[String]

  def tryRun(
    cache: Cache[Task],
    versionOpt: Option[String],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    allowExecve: Boolean
  ): Either[BuildException, Int] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val launcher = value(PgpExternalCommand.launcher(archiveCache, versionOpt, logger))

    val command = Seq(launcher.toString) ++ externalCommand ++ args

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
    logger: Logger
  ): Either[BuildException, String] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val launcher = value(PgpExternalCommand.launcher(archiveCache, versionOpt, logger))

    val command = Seq(launcher.toString) ++ externalCommand ++ args

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

    val retCode = tryRun(
      options.coursier.coursierCache(logger.coursierLogger("")),
      options.signingCliVersion.map(_.trim).filter(_.nonEmpty),
      remainingArgs,
      Map(),
      logger,
      allowExecve = true
    ).orExit(logger)

    if (retCode != 0)
      sys.exit(retCode)
  }
}

object PgpExternalCommand {
  def launcher(
    archiveCache: ArchiveCache[Task],
    versionOpt: Option[String],
    logger: Logger
  ): Either[BuildException, os.Path] = {

    val platformSuffix = FetchExternalBinary.platformSuffix()
    val version = versionOpt
      .getOrElse(Constants.scalaCliSigningVersion)
    val (tag, changing) =
      if (version == "latest") ("launchers", true)
      else ("v" + version, false)
    val ext = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/scala-cli/scala-cli-signing/releases/download/$tag/scala-cli-signing-$platformSuffix$ext"

    FetchExternalBinary.fetch(url, changing, archiveCache, logger, "scala-cli-signing")
  }
}
