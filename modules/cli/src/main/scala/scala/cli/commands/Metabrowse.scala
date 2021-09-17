package scala.cli.commands

import java.io.{IOException, File, FileInputStream, FileOutputStream}
import java.net.ServerSocket
import java.nio.file.Path
import java.util.UUID
import java.util.zip.GZIPInputStream

import caseapp._
import coursier.jvm.ArchiveType
import coursier.util.{Artifact, Task}

import scala.build.{Build, BuildThreads, Inputs, Logger, Os}
import scala.build.internal.Runner
import scala.build.options.BuildOptions
import scala.cli.internal.FetchExternalBinary
import scala.concurrent.ExecutionContext.{global => ec}
import scala.util.Properties

object Metabrowse extends ScalaCommand[MetabrowseOptions] {
  override def group = "Miscellaneous"
  override def names = List(
    List("browse"),
    List("metabrowse")
  )

  override def sharedOptions(options: MetabrowseOptions) = Some(options.shared)

  def run(options: MetabrowseOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args)

    val logger = options.shared.logger

    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val (build, _) =
      Build.build(inputs, options.buildOptions, bloopRifleConfig, logger, crossBuilds = false)
        .orExit(logger)

    val successfulBuild = build match {
      case f: Build.Failed =>
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
          options.metabrowseBinaryUrl(successfulBuild.options.scalaParams.scalaVersion)
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

        val rtJarLocation = successfulBuild.options.javaHomeLocation() / "jre" / "lib" / "rt.jar"

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
      val sv = successfulBuild.options.scalaParams.scalaVersion
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

  private def randomPort(): Int = {
    val s    = new ServerSocket(0)
    val port = s.getLocalPort
    s.close()
    port
  }
}
