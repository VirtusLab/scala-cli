package scala.cli.commands.pgp

import coursier.Repositories
import coursier.cache.{ArchiveCache, Cache, FileCache}
import coursier.core.Version
import coursier.util.Task
import dependency._

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, ScalaJsLinkingError}
import scala.build.internal.Util.{DependencyOps, ModuleOps}
import scala.build.internal.{
  Constants,
  ExternalBinary,
  ExternalBinaryParams,
  FetchExternalBinary,
  Runner,
  ScalaJsLinkerConfig
}
import scala.build.options.scalajs.ScalaJsLinkerOptions
import scala.build.{Logger, Positioned, options => bo}
import scala.cli.ScalaCli
import scala.cli.commands.util.JvmUtils
import scala.util.Properties

abstract class PgpExternalCommand extends ExternalCommand {
  def progName: String = ScalaCli.progName
  def externalCommand: Seq[String]

  def tryRun(
    cache: FileCache[Task],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    allowExecve: Boolean,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Int] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(
      cache,
      archiveCache,
      logger,
      javaCommand,
      signingCliOptions
    ))

    val command = binary ++ externalCommand ++ args

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
    cache: FileCache[Task],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, String] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(
      cache,
      archiveCache,
      logger,
      javaCommand,
      signingCliOptions
    ))

    val command = binary ++ externalCommand ++ args

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
      remainingArgs,
      Map(),
      logger,
      allowExecve = true,
      () =>
        JvmUtils.javaOptions(options.jvm).orExit(logger).javaHome(
          ArchiveCache().withCache(cache),
          cache,
          logger.verbosity
        ).value.javaCommand,
      options.scalaSigning.cliOptions()
    ).orExit(logger)

    if (retCode != 0)
      sys.exit(retCode)
  }
}

object PgpExternalCommand {
  def launcher(
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    logger: Logger,
    javaCommand: () => String,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Seq[String]] = either {

    val version =
      signingCliOptions.signingCliVersion
        .getOrElse(Constants.scalaCliSigningVersion)
    val ver              = if (version.startsWith("latest")) "latest.release" else version
    val signingMainClass = "scala.cli.signing.ScalaCliSigning"
    val jvmSigningDep =
      dep"${Constants.scalaCliSigningOrganization}:${Constants.scalaCliSigningName}_3:$ver"

    if (signingCliOptions.useJvm.getOrElse(false)) {
      val extraRepos =
        if (version.endsWith("SNAPSHOT"))
          Seq(Repositories.sonatype("snapshots"))
        else
          Nil

      val signingClassPath = value {
        scala.build.Artifacts.fetch0(
          Positioned.none(Seq(jvmSigningDep.toCs)),
          extraRepos,
          None,
          Nil,
          logger,
          cache,
          None
        )
      }.files

      val command = Seq[os.Shellable](
        javaCommand(),
        signingCliOptions.javaArgs,
        "-cp",
        signingClassPath.map(_.getAbsolutePath).mkString(File.pathSeparator),
        signingMainClass
      )

      command.flatMap(_.value)
    }
    else {
      val platformSuffix = FetchExternalBinary.platformSuffix()
      val (tag, changing) =
        if (version == "latest") ("launchers", true)
        else ("v" + version, false)
      val ext = if (Properties.isWin) ".zip" else ".gz"
      val url =
        s"https://github.com/scala-cli/scala-cli-signing/releases/download/$tag/scala-cli-signing-$platformSuffix$ext"
      val params = ExternalBinaryParams(
        url,
        changing,
        "scala-cli-signing",
        Seq(jvmSigningDep),
        signingMainClass
      )
      val binary = value {
        FetchExternalBinary.fetch(params, archiveCache, logger, javaCommand)
      }
      binary.command
    }
  }
}
