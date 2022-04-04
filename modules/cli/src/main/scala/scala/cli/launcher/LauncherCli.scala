package scala.cli.launcher
import coursier.Repositories
import coursier.cache.FileCache
import coursier.core.Version
import coursier.util.{Artifact, Task}
import dependency._

import scala.build.internal.CsLoggerUtil.CsCacheExtensions
import scala.build.internal.{OsLibc, Runner}
import scala.build.options.{BuildOptions, JavaOptions}
import scala.build.{Artifacts, Os, Positioned}
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.{CoursierOptions, LoggingOptions}
import scala.concurrent.duration._
import scala.util.Properties
import scala.util.control.NonFatal

object LauncherCli {

  def runAndExit(version: String, options: LauncherOptions, remainingArgs: Seq[String]): Nothing = {

    val logger          = LoggingOptions().logger
    val cache           = CoursierOptions().coursierCache(logger.coursierLogger(""))
    val scalaVersion    = options.cliScalaVersion.getOrElse(Properties.versionNumberString)
    val scalaParameters = ScalaParameters(scalaVersion)
    val snapshotsRepo   = Seq(Repositories.central.root, Repositories.sonatype("snapshots").root)

    val cliVersion: String =
      if (version == "nightly") resolveNightlyScalaCliVersion(cache, scalaParameters) else version
    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli::cli:$cliVersion")

    val fetchedScalaCli =
      Artifacts.fetch(
        Positioned.none(scalaCliDependency),
        snapshotsRepo,
        scalaParameters,
        logger,
        cache.withMessage(s"Fetching Scala CLI $cliVersion"),
        None
      ) match {
        case Right(value) => value
        case Left(value) =>
          System.err.println(value.message)
          sys.exit(1)
      }

    val scalaCli =
      fetchedScalaCli.fullDetailedArtifacts.collect { case (_, _, _, Some(f)) =>
        f.toPath.toFile
      }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"))
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

  def resolveNightlyScalaCliVersion(
    cache: FileCache[Task],
    scalaParameters: ScalaParameters
  ): String = {

    val snapshotRepoUrl =
      s"https://oss.sonatype.org/content/repositories/snapshots/org/virtuslab/scala-cli/cli_${scalaParameters.scalaBinaryVersion}/"
    val artifact = Artifact(snapshotRepoUrl).withChanging(true)
    val res = cache.logger.use {
      try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(cache.ec)
      catch {
        case NonFatal(e) => throw new Exception(e)
      }
    }

    res match {
      case Left(_) =>
        System.err.println("Unable to find nightly Scala CLI version")
        sys.exit(1)
      case Right(f) =>
        val snapshotRepoPage = os.read(os.Path(f, Os.pwd))
        val rawVersions      = coursier.CoursierUtil.rawVersions(snapshotRepoUrl, snapshotRepoPage)
        val versions         = rawVersions.map(Version(_))
        val nightlyVersion   = versions.max

        nightlyVersion.repr
    }

  }

}
