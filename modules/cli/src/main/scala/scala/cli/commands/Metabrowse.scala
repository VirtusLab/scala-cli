package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.internal.{Constants, FetchExternalBinary, Runner}
import scala.build.{Build, BuildThreads, Logger}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.packaging.Library
import scala.util.Properties
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps

object Metabrowse extends ScalaCommand[MetabrowseOptions] {
  override def hidden     = true
  override def inSipScala = false
  override def group      = "Miscellaneous"
  override def names = List(
    List("browse"),
    List("metabrowse")
  )

  override def sharedOptions(options: MetabrowseOptions) = Some(options.shared)

  private def metabrowseBinaryUrl(
    scalaVersion: String,
    options: MetabrowseOptions
  ): (String, Boolean) = {
    import options._
    val osArchSuffix0 = osArchSuffix.map(_.trim).filter(_.nonEmpty)
      .getOrElse(FetchExternalBinary.platformSuffix(supportsMusl = false))
    val metabrowseTag0           = metabrowseTag.getOrElse("latest")
    val metabrowseGithubOrgName0 = metabrowseGithubOrgName.getOrElse("alexarchambault/metabrowse")
    val metabrowseExtension0     = if (Properties.isWin) ".zip" else ".gz"
    val url =
      s"https://github.com/$metabrowseGithubOrgName0/releases/download/$metabrowseTag0/metabrowse-$scalaVersion-$osArchSuffix0$metabrowseExtension0"
    (url, !metabrowseTag0.startsWith("v"))
  }

  def run(options: MetabrowseOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val logger = options.shared.logger
    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val baseOptions = options.shared.buildOptions()
    val initialBuildOptions = baseOptions.copy(
      classPathOptions = baseOptions.classPathOptions.copy(
        fetchSources = Some(true)
      ),
      javaOptions = baseOptions.javaOptions.copy(
        jvmIdOpt = baseOptions.javaOptions.jvmIdOpt.orElse(Some("8"))
      )
    )
    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)
    val configDb = ConfigDb.open(options.shared.directories.directories)
      .orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val builds =
      Build.build(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = false,
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics
      )
        .orExit(logger)

    val successfulBuild = builds.main match {
      case _: Build.Failed =>
        System.err.println("Build failed")
        sys.exit(1)
      case s: Build.Successful => s
    }

    Library.withLibraryJar(successfulBuild) { jar =>
      Package.withSourceJar(successfulBuild, System.currentTimeMillis()) { sourceJar =>
        runServer(options, logger, successfulBuild, jar, sourceJar)
      }
    }
  }

  def runServer(
    options: MetabrowseOptions,
    logger: Logger,
    successfulBuild: Build.Successful,
    jar: os.Path,
    sourceJar: os.Path
  ): Unit = {

    val launcher = options.metabrowseLauncher
      .filter(_.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse {
        val sv = successfulBuild.scalaParams
          .map(_.scalaVersion)
          .getOrElse(Constants.defaultScalaVersion)
        val (url, changing) =
          metabrowseBinaryUrl(sv, options)
        FetchExternalBinary.fetch(
          url,
          changing,
          successfulBuild.options.archiveCache,
          logger,
          "metabrowse"
        )
      }

    logger.debug(s"Using metabrowse launcher $launcher")

    val extraJars =
      if (options.addRtJar.getOrElse(true)) {

        val rtJarLocation =
          successfulBuild.options.javaHomeLocation().value / "jre" / "lib" / "rt.jar"

        val rtJarOpt =
          if (os.isFile(rtJarLocation)) Some(rtJarLocation)
          else None

        if (rtJarOpt.isEmpty && options.shared.logging.verbosity >= 0)
          System.err.println(s"Warning: could not find $rtJarLocation")

        rtJarOpt.toSeq
      }
      else Nil

    val classPath = jar :: (successfulBuild.artifacts.classPath ++ extraJars).toList
    val sources   = sourceJar :: successfulBuild.artifacts.sourcePath.toList

    logger.debug {
      val newLine = System.lineSeparator()
      val b       = new StringBuilder
      b.append("Class path:")
      b.append(newLine)
      for (jar <- classPath) {
        b.append("  ")
        b.append(jar.toString)
        b.append(newLine)
      }
      b.result()
    }

    logger.debug {
      val newLine = System.lineSeparator()
      val b       = new StringBuilder
      b.append("Source path:")
      b.append(newLine)
      for (jar <- sources) {
        b.append("  ")
        b.append(jar.toString)
        b.append(newLine)
      }
      b.result()
    }

    def defaultDialect = {
      val sv = successfulBuild.scalaParams
        .map(_.scalaVersion)
        .getOrElse(Constants.defaultScalaVersion)
      if (sv.startsWith("2.12.")) "Scala212"
      else if (sv.startsWith("2.13.")) "Scala213"
      else "Scala3"
    }

    val message = WatchUtil.waitMessage("Metabrowse server running at http://{HOST}:{PORT}")

    val command = Seq(
      launcher.toString,
      "--class-path",
      classPath.map(_.toString).mkString(File.pathSeparator),
      "--source-path",
      sources.map(_.toString).mkString(File.pathSeparator),
      "--host",
      options.host,
      "--port",
      options.port.toString,
      "--dialect",
      options.metabrowseDialect.map(_.trim).filter(_.nonEmpty).getOrElse(defaultDialect),
      "--message",
      message
    )

    Runner.maybeExec("metabrowse", command, logger)
  }

}
