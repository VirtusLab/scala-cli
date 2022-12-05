package scala.cli.launcher

import coursier.Repositories
import coursier.cache.FileCache
import coursier.core.Version
import coursier.util.{Artifact, Task}
import dependency.*

import scala.build.internal.CsLoggerUtil.CsCacheExtensions
import scala.build.internal.{Constants, OsLibc, Runner}
import scala.build.options.ScalaVersionUtil.fileWithTtl0
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Artifacts, Os, Positioned}
import scala.cli.ScalaCli
import scala.cli.commands.shared.{CoursierOptions, LoggingOptions}
import scala.concurrent.duration.*
import scala.util.control.NonFatal

object LauncherCli {

  def runAndExit(version: String, options: LauncherOptions, remainingArgs: Seq[String]): Nothing = {

    val logger          = LoggingOptions().logger
    val cache           = CoursierOptions().coursierCache(logger.coursierLogger(""))
    val scalaVersion    = options.cliScalaVersion.getOrElse(scalaCliScalaVersion(version))
    val scalaParameters = ScalaParameters(scalaVersion)
    val snapshotsRepo   = Seq(Repositories.central, Repositories.sonatype("snapshots"))

    val cliVersion: String =
      if (version == "nightly") resolveNightlyScalaCliVersion(cache, scalaParameters) else version
    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli::cli:$cliVersion")

    val fetchedScalaCli =
      Artifacts.fetch(
        Positioned.none(scalaCliDependency),
        snapshotsRepo,
        Some(scalaParameters),
        logger,
        cache.withMessage(s"Fetching ${ScalaCli.fullRunnerName} $cliVersion"),
        None
      ) match {
        case Right(value) => value
        case Left(value) =>
          System.err.println(value.message)
          sys.exit(1)
      }

    val scalaCli = fetchedScalaCli.fullDetailedArtifacts.collect {
      case (_, _, _, Some(f)) =>
        os.Path(f, os.pwd)
    }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17")).map(Positioned.none)
      )
    )

    val exitCode =
      Runner.runJvm(
        buildOptions.javaHome().value.javaCommand,
        buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        scalaCli,
        "scala.cli.ScalaCli",
        remainingArgs,
        logger,
        allowExecve = true
      ).waitFor()

    sys.exit(exitCode)
  }

  def scalaCliScalaVersion(cliVersion: String): String =
    if (cliVersion == "nightly") Constants.defaultScalaVersion
    else if (Version(cliVersion) <= Version("0.1.2")) Constants.defaultScala212Version
    else if (Version(cliVersion) <= Version("0.1.4")) Constants.defaultScala213Version
    else Constants.defaultScalaVersion

  def resolveNightlyScalaCliVersion(
    cache: FileCache[Task],
    scalaParameters: ScalaParameters
  ): String = {

    val snapshotRepoUrl =
      s"https://oss.sonatype.org/content/repositories/snapshots/org/virtuslab/scala-cli/cli_${scalaParameters.scalaBinaryVersion}/"
    val artifact = Artifact(snapshotRepoUrl).withChanging(true)
    cache.fileWithTtl0(artifact) match {
      case Left(_) =>
        System.err.println(s"Unable to find nightly ${ScalaCli.fullRunnerName} version")
        sys.exit(1)
      case Right(f) =>
        val snapshotRepoPage = os.read(os.Path(f, Os.pwd))
        val rawVersions      = coursier.CoursierUtil.rawVersions(snapshotRepoUrl, snapshotRepoPage)
        val versions         = rawVersions.map(Version(_))

        if (versions.isEmpty)
          sys.error(s"No versions found in $snapshotRepoUrl (locally at $f)")
        else
          versions.max.repr
    }

  }

}
