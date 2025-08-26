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
import scala.build.{Artifacts, Os, Positioned, RepositoryUtils}
import scala.cli.ScalaCli
import scala.cli.commands.shared.{CoursierOptions, LoggingOptions}
import scala.xml.XML

object LauncherCli {
  def runAndExit(version: String, options: LauncherOptions, remainingArgs: Seq[String]): Nothing = {
    val logger          = LoggingOptions().logger
    val cache           = CoursierOptions().coursierCache(logger.coursierLogger(""))
    val scalaVersion    = options.cliScalaVersion.getOrElse(scalaCliScalaVersion(version))
    val scalaParameters = ScalaParameters(scalaVersion)
    val snapshotsRepo   = Seq(
      Repositories.central,
      Repositories.sonatype("snapshots"),
      RepositoryUtils.snapshotsRepository,
      RepositoryUtils.scala3NightlyRepository
    )

    val cliVersion: String =
      if version == "nightly"
      then resolveNightlyScalaCliVersion(cache, scalaParameters.scalaBinaryVersion)
      else version
    val scalaCliDependency = Seq(dep"org.virtuslab.scala-cli::cli:$cliVersion")

    val fetchedScalaCli =
      Artifacts.fetchAnyDependencies(
        dependencies = scalaCliDependency.map(Positioned.none),
        extraRepositories = snapshotsRepo,
        paramsOpt = Some(scalaParameters),
        logger = logger,
        cache = cache.withMessage(s"Fetching ${ScalaCli.fullRunnerName} $cliVersion"),
        classifiersOpt = None
      ) match {
        case Right(value) => value
        case Left(value)  =>
          System.err.println(value.message)
          sys.exit(1)
      }

    val scalaCli = fetchedScalaCli.fullDetailedArtifacts.collect {
      case (_, _, _, Some(f)) => os.Path(f, os.pwd)
    }

    val buildOptions = BuildOptions(
      javaOptions = JavaOptions(
        jvmIdOpt = Some(OsLibc.defaultJvm(OsLibc.jvmIndexOs)).map(Positioned.none)
      )
    )

    val exitCode =
      Runner.runJvm(
        javaCommand = buildOptions.javaHome().value.javaCommand,
        javaArgs = buildOptions.javaOptions.javaOpts.toSeq.map(_.value.value),
        classPath = scalaCli,
        mainClass = "scala.cli.ScalaCli",
        args = remainingArgs,
        logger = logger,
        allowExecve = true
      ).waitFor()

    sys.exit(exitCode)
  }

  def scalaCliScalaVersion(cliVersion: String): String =
    if cliVersion == "nightly" then Constants.defaultScalaVersion
    else if Version(cliVersion) <= Version("0.1.2") then Constants.defaultScala212Version
    else if Version(cliVersion) <= Version("0.1.4") then Constants.defaultScala213Version
    else Constants.defaultScalaVersion

  def resolveNightlyScalaCliVersion(
    cache: FileCache[Task],
    scalaBinaryVersion: String
  ): String = {
    val cliSubPath       = s"org/virtuslab/scala-cli/cli_$scalaBinaryVersion"
    val mavenMetadataUrl =
      s"${RepositoryUtils.snapshotsRepositoryUrl}/$cliSubPath/maven-metadata.xml"
    val artifact = Artifact(mavenMetadataUrl).withChanging(true)
    cache.fileWithTtl0(artifact) match {
      case Left(_) =>
        System.err.println(s"Unable to find nightly ${ScalaCli.fullRunnerName} version")
        sys.exit(1)
      case Right(mavenMetadataXml) =>
        val metadataXmlContent = os.read(os.Path(mavenMetadataXml, Os.pwd))
        val parsed             = XML.loadString(metadataXmlContent)
        val rawVersions        = (parsed \ "versioning" \ "versions" \ "version").map(_.text)
        val versions           = rawVersions.map(Version(_))
        if versions.isEmpty
        then sys.error(s"No versions found in $mavenMetadataUrl (locally at $mavenMetadataXml)")
        else versions.max.repr
    }
  }
}
