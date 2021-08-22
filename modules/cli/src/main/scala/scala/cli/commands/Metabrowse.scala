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

    val build = Build.build(inputs, options.buildOptions, bloopRifleConfig, logger, options.shared.directories.directories)

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

    def defaultLauncher() = {

      val (url, changing) = options.metabrowseBinaryUrl(successfulBuild.options.scalaParams.scalaVersion)
      val cache = options.shared.coursierCache
      val f = cache.logger.use {
        logger.log(s"Getting $url")
        cache.file(Artifact(url).withChanging(changing)).run.flatMap {
          case Left(e) => Task.fail(e)
          case Right(f) => Task.point(os.Path(f, os.pwd))
        }.unsafeRun()(cache.ec)
      }
      logger.debug(s"$url is available locally at $f")

      // FIXME Once coursier has proper support for extracted archives in cache, use it instead of those hacks
      if (f.last.endsWith(".zip")) {
        val baseDir = f / os.up
        val dir = baseDir / s".${f.last.stripSuffix(".zip")}-content"
        if (os.exists(dir))
          logger.debug(s"Found $dir")
        else {
          logger.debug(s"Unzipping $f under $dir")
          val tmpDir = baseDir / s".${f.last.stripSuffix(".zip")}-content-${UUID.randomUUID()}"
          try {
            coursier.jvm.UnArchiver.default().extract(ArchiveType.Zip, f.toIO, tmpDir.toIO, overwrite = false)
            if (!os.exists(dir)) {
              try os.move(tmpDir, dir, atomicMove = true)
              catch {
                case ex: IOException =>
                  if (!os.exists(dir))
                    throw new Exception(ex)
              }
            }
          } finally {
            try os.remove.all(tmpDir)
            catch {
              case _: IOException if Properties.isWin =>
            }
          }
        }

        val dirContent = os.list(dir)
        if (dirContent.length == 1) dirContent.head
        else dirContent.filter(_.last.startsWith("metabrowse")).head
      } else if (f.last.endsWith(".gz")) {
        val dest = f / os.up / s".${f.last.stripSuffix(".gz")}"
        if (os.exists(dest))
          logger.debug(s"Found $dest")
        else {
          logger.debug(s"Uncompression $f at $dest")
          var fis: FileInputStream = null
          var fos: FileOutputStream = null
          var gzis: GZIPInputStream = null
          try {
            fis = new FileInputStream(f.toIO)
            gzis = new GZIPInputStream(fis)
            fos = new FileOutputStream(dest.toIO)

            val buf = Array.ofDim[Byte](16*1024)
            var read = -1
            while ({
              read = gzis.read(buf)
              read >= 0
            }) {
              if (read > 0)
                fos.write(buf, 0, read)
            }
            fos.flush()
          } finally {
            if (gzis != null) gzis.close()
            if (fos != null) fos.close()
            if (fis != null) fis.close()
          }
        }
        dest
      } else
        f
    }

    val launcher = options.metabrowseLauncher
      .filter(_.nonEmpty)
      .map(os.Path(_, os.pwd))
      .getOrElse(defaultLauncher())

    logger.debug(s"Using metabrowse launcher $launcher")

    if (!Properties.isWin)
      os.perms.set(launcher, "rwxr-xr-x")

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

    def defaultDialect = {
      val sv = successfulBuild.options.scalaParams.scalaVersion
      if (sv.startsWith("2.12.")) "Scala212"
      else if (sv.startsWith("2.13.")) "Scala213"
      else "Scala3"
    }

    val message = WatchUtil.waitMessage("Metabrowse server running at http://{HOST}:{PORT}")

    val command = Seq(
      launcher.toString,
      "--class-path", classPath.map(_.toString).mkString(File.pathSeparator),
      "--source-path", sources.map(_.toString).mkString(File.pathSeparator),
      "--host", options.host,
      "--port", options.port.toString,
      "--dialect", options.metabrowseDialect.map(_.trim).filter(_.nonEmpty).getOrElse(defaultDialect),
      "--message", message
    )

    Runner.run("metabrowse", command, logger, allowExecve = true)
  }

  private def randomPort(): Int = {
    val s = new ServerSocket(0)
    val port = s.getLocalPort
    s.close()
    port
  }
}
