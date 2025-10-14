package scala.cli.commands.pgp

import coursier.Repositories
import coursier.cache.{ArchiveCache, FileCache}
import coursier.util.Task
import dependency.*

import java.io.File

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.BuildException
import scala.build.internal.Util.DependencyOps
import scala.build.internal.{
  Constants,
  ExternalBinary,
  ExternalBinaryParams,
  FetchExternalBinary,
  Runner
}
import scala.build.{Logger, Positioned, RepositoryUtils, options as bo}
import scala.cli.ScalaCli
import scala.cli.commands.shared.{CoursierOptions, SharedJvmOptions}
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
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Int] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(
      cache,
      archiveCache,
      logger,
      jvmOptions,
      coursierOptions,
      signingCliOptions
    ))

    val command = binary ++ externalCommand ++ args

    Runner.run0(
      progName,
      command,
      logger,
      allowExecve = allowExecve,
      cwd = None,
      extraEnv = extraEnv,
      inheritStreams = true
    ).waitFor()
  }

  def output(
    cache: FileCache[Task],
    args: Seq[String],
    extraEnv: Map[String, String],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, String] = either {

    val archiveCache = ArchiveCache().withCache(cache)

    val binary = value(PgpExternalCommand.launcher(
      cache,
      archiveCache,
      logger,
      jvmOptions,
      coursierOptions,
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

    val logger = options.global.logging.logger

    val cache   = options.coursier.coursierCache(logger.coursierLogger(""))
    val retCode = tryRun(
      cache,
      remainingArgs,
      Map(),
      logger,
      allowExecve = true,
      options.jvm,
      options.coursier,
      options.scalaSigning.cliOptions()
    ).orExit(logger)

    if (retCode != 0)
      sys.exit(retCode)
  }
}

object PgpExternalCommand {
  val scalaCliSigningJvmVersion: Int = Constants.signingCliJvmVersion

  def launcher(
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    logger: Logger,
    jvmOptions: SharedJvmOptions,
    coursierOptions: CoursierOptions,
    signingCliOptions: bo.ScalaSigningCliOptions
  ): Either[BuildException, Seq[String]] = {
    val javaCommand = () =>
      JvmUtils.getJavaCmdVersionOrHigher(
        scalaCliSigningJvmVersion,
        jvmOptions,
        coursierOptions
      ).orThrow.javaCommand

    launcher(
      cache,
      archiveCache,
      logger,
      javaCommand,
      signingCliOptions
    )
  }

  def launcher(
    cache: FileCache[Task],
    archiveCache: ArchiveCache[Task],
    logger: Logger,
    buildOptions: bo.BuildOptions
  ): Either[BuildException, Seq[String]] = {
    val javaCommand = () =>
      JvmUtils.getJavaCmdVersionOrHigher(
        scalaCliSigningJvmVersion,
        buildOptions
      ).orThrow.javaCommand

    launcher(
      cache,
      archiveCache,
      logger,
      javaCommand,
      buildOptions.notForBloopOptions.publishOptions.signingCli
    )
  }

  private def launcher(
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
    val jvmSigningDep    =
      dep"${Constants.scalaCliSigningOrganization}:${Constants.scalaCliSigningName}_3:$ver"

    if (signingCliOptions.forceJvm.getOrElse(false)) {
      val extraRepos =
        if version.endsWith("SNAPSHOT") then
          Seq(
            Repositories.sonatype("snapshots"),
            Repositories.sonatypeS01("snapshots"),
            RepositoryUtils.snapshotsRepository,
            RepositoryUtils.scala3NightlyRepository
          )
        else Nil

      val (_, signingRes) = value {
        scala.build.Artifacts.fetchCsDependencies(
          dependencies = Seq(Positioned.none(jvmSigningDep.toCs)),
          extraRepositories = extraRepos,
          forceScalaVersionOpt = None,
          forcedVersions = Nil,
          logger = logger,
          cache = cache,
          classifiersOpt = None
        )
      }
      val signingClassPath = signingRes.files

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
      val platformSuffix  = FetchExternalBinary.platformSuffix()
      val (tag, changing) =
        if (version == "latest") ("launchers", true)
        else ("v" + version, false)
      val ext = if (Properties.isWin) ".zip" else ".gz"
      val url =
        s"https://github.com/VirtusLab/scala-cli-signing/releases/download/$tag/scala-cli-signing-$platformSuffix$ext"
      val params = ExternalBinaryParams(
        url,
        changing,
        "scala-cli-signing",
        Seq(jvmSigningDep),
        signingMainClass
      )
      val binary: ExternalBinary = value {
        FetchExternalBinary.fetch(params, archiveCache, logger, javaCommand)
      }
      binary.command
    }
  }

}
