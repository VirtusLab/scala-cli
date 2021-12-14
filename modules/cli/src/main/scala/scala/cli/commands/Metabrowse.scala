package scala.cli.commands

import caseapp._

import java.io.File
import java.nio.file.Path

import scala.build.internal.Runner
import scala.build.{Build, Logger}
import scala.cli.CurrentParams
import scala.cli.internal.FetchExternalBinary

object Metabrowse extends ScalaCommand[MetabrowseOptions] {
  override def group = "Miscellaneous"
  override def names = List(
    List("browse"),
    List("metabrowse")
  )

  override def sharedOptions(options: MetabrowseOptions) = Some(options.shared)

  def run(options: MetabrowseOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val logger = options.shared.logger

    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val builds =
      Build.build(inputs, options.buildOptions, bloopRifleConfig, logger, crossBuilds = false)
        .orExit(logger)

    val successfulBuild = builds.main match {
      case _: Build.Failed =>
        System.err.println("Build failed")
        sys.exit(1)
      case s: Build.Successful => s
    }

    Package.withLibraryJar(successfulBuild) { jar =>
      Package.withSourceJar(successfulBuild, System.currentTimeMillis()) { sourceJar =>
        runServer(options, logger, successfulBuild, jar, sourceJar)
      }
    }
  }

  def runServer(
    options: MetabrowseOptions,
    logger: Logger,
    successfulBuild: Build.Successful,
    jar: Path,
    sourceJar: Path
  ): Unit = {

    val launcher = options.metabrowseLauncher
      .filter(_.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse {
        val (url, changing) =
          options.metabrowseBinaryUrl(successfulBuild.scalaParams.scalaVersion)
        FetchExternalBinary.fetch(
          url,
          changing,
          options.shared.coursierCache,
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
          if (os.isFile(rtJarLocation)) Some(rtJarLocation.toNIO)
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
      val sv = successfulBuild.scalaParams.scalaVersion
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

    Runner.run("metabrowse", command, logger, allowExecve = true)
  }

}
