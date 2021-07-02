package scala.cli.commands

import java.net.ServerSocket
import java.nio.file.Path

import caseapp._
import metabrowse.server.{MetabrowseServer, Sourcepath}

import scala.build.{Build, Inputs, Logger, Os}
import scala.concurrent.ExecutionContext.{global => ec}
import scala.util.Properties

object Metabrowse extends ScalaCommand[MetabrowseOptions] {
  override def group = "Miscellaneous"
  override def names = List(
    List("browse"),
    List("metabrowse")
  )

  override def sharedOptions(options: MetabrowseOptions) = Some(options.shared)

  def isNativeImage = sys.props.contains("org.graalvm.nativeimage.imagecode")

  def run(options: MetabrowseOptions, args: RemainingArgs): Unit = {

    // silence undertow logging
    sys.props("org.jboss.logging.provider") = "slf4j"

    val inputs = options.shared.inputsOrExit(args)

    val logger = options.shared.logger

    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val build = Build.build(inputs, options.buildOptions, bloopRifleConfig, logger, Os.pwd)

    if (build.artifacts.params.scalaVersion != Properties.versionNumberString && options.shared.logging.verbosity >= 0)
      System.err.println(s"Warning: browse command should only work with Scala version ${Properties.versionNumberString}")

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

    val extraJars =
      if (options.addRtJar.getOrElse(isNativeImage)) {

        val rtJarLocation = successfulBuild.options.javaHomeLocation() / "jre" / "lib" / "rt.jar"

        val rtJarOpt =
          if (os.isFile(rtJarLocation)) Some(rtJarLocation.toNIO)
          else None

        if (rtJarOpt.isEmpty && isNativeImage && options.shared.logging.verbosity >= 0)
          System.err.println(s"Warning: could not find $rtJarLocation")

        rtJarOpt.toSeq
      }
      else Nil

    val classPath = jar :: (successfulBuild.artifacts.classPath ++ extraJars).toList
    val sources = sourceJar :: successfulBuild.artifacts.sourcePath.toList

    logger.debug {
      val newLine = System.lineSeparator()
      val b = new StringBuilder
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
      val b = new StringBuilder
      b.append("Source path:")
      b.append(newLine)
      for (jar <- sources) {
        b.append("  ")
        b.append(jar.toString)
        b.append(newLine)
      }
      b.result()
    }

    val sourcePath = Sourcepath(classpath = classPath, sources = sources)

    val host = options.host
    val port =
      if (options.port > 0) options.port
      else randomPort()

    val dialect = scala.meta.dialects.Scala213

    val server = new MetabrowseServer(
      dialect = dialect,
      scalacOptions = successfulBuild.project.scalaCompiler.scalacOptions.toList,
      host = host,
      port = port
    )

    logger.log(s"Starting metabrowse server at http://$host:$port")
    logger.debug {
      val nl = System.lineSeparator()
      "Initial source path" + nl +
        "  Classpath" + nl +
        sourcePath.classpath.map("    " + _).mkString(nl) + nl +
        "  Sources" + nl +
        sourcePath.sources.map("    " + _).mkString(nl)
    }

    server.start(sourcePath)

    try {
      WatchUtil.printWaitMessage(s"Metabrowse server running at http://$host:$port")
      WatchUtil.waitForCtrlC()
    } finally {
      server.stop()
    }
  }

  private def randomPort(): Int = {
    val s = new ServerSocket(0)
    val port = s.getLocalPort
    s.close()
    port
  }
}
