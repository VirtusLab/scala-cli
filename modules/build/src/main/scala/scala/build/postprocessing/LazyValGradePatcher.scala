package scala.build.postprocessing

import coursier.LocalRepositories
import coursier.cache.FileCache
import dependency.*
import os.Path

import java.io.{ByteArrayOutputStream, PrintStream}
import java.net.URLClassLoader
import java.nio.file.Path as NioPath
import java.security.MessageDigest

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.internal.CsLoggerUtil.*
import scala.build.options.BuildOptions
import scala.build.{Artifacts, Directories, Logger, Positioned}

object LazyValGradePatcher {

  private val cacheDir = Directories.directories.cacheDir / "lazyvalgrade"

  def transformClassPath(
    classPath: Seq[Path],
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    if options.notForBloopOptions.lazyValGradeOpt.contains(true) then
      either {
        val toolClassPath = value(fetchToolClassPath(options, logger))
        val toolLoader    = isolatedToolLoader(toolClassPath)
        // lazyvalgrade logs to stdout/stderr via its own bundled logging; capture it so it doesn't
        // pollute the user program's output, routing anything it prints to the debug log instead.
        value {
          captureStdio(logger) {
            classPath.iterator.map { entry =>
              if entry.ext == "jar" then patchJar(entry, toolLoader, logger)
              else Right(entry)
            }.sequence0
          }
        }
      }
    else Right(classPath)

  private def captureStdio[T](logger: Logger)(f: => T): T = {
    val outBuffer   = new ByteArrayOutputStream
    val errBuffer   = new ByteArrayOutputStream
    val originalOut = System.out
    val originalErr = System.err
    System.setOut(new PrintStream(outBuffer, true))
    System.setErr(new PrintStream(errBuffer, true))
    try f
    finally {
      System.setOut(originalOut)
      System.setErr(originalErr)
      val captured = (outBuffer.toString ++ errBuffer.toString).trim
      if captured.nonEmpty then logger.debug(captured)
    }
  }

  private def fetchToolClassPath(
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    either {
      val cache        = options.internal.cache.getOrElse(FileCache())
      val repositories = value(options.finalRepositories) ++ Seq(LocalRepositories.ivy2Local)
      val scalaParams  = value(options.scalaParams).getOrElse(
        ScalaParameters(Constants.defaultScalaVersion)
      )
      val lazyValGradeDeps =
        Seq(
          dep"${Constants.lazyvalgradeOrganization}::${Constants.lazyvalgradeModuleName}:${Constants.lazyvalgradeVersion}"
        )
      val artifacts = value(
        Artifacts.artifacts(
          lazyValGradeDeps.map(Positioned.none),
          repositories,
          Some(scalaParams),
          logger,
          cache.withMessage(s"Downloading lazyvalgrade ${Constants.lazyvalgradeVersion}")
        )
      )
      artifacts.map(_._2)
    }

  /** Loads lazyvalgrade in an isolated classloader (null parent) so that its ASM dependency does
    * not clash with the one used by scala-cli itself.
    */
  private def isolatedToolLoader(toolClassPath: Seq[Path]): URLClassLoader =
    new URLClassLoader(toolClassPath.map(_.toNIO.toUri.toURL).toArray, null)

  private def patchJar(
    jar: Path,
    toolLoader: URLClassLoader,
    logger: Logger
  ): Either[BuildException, Path] =
    either {
      val digest    = sha1(jar)
      val cachedDir = cacheDir / Constants.lazyvalgradeVersion / digest
      val cached    = cachedDir / jar.last
      if os.exists(cached) then cached
      else {
        os.makeDir.all(cachedDir)
        value(runJarProcessor(jar, cached, toolLoader))
        logger.debug(s"Patched lazy vals in $jar -> $cached")
        cached
      }
    }

  /** Invokes `lazyvalgrade.jar.JarProcessor.process` reflectively through the isolated loader. */
  private def runJarProcessor(
    input: Path,
    output: Path,
    toolLoader: URLClassLoader
  ): Either[BuildException, Unit] =
    try {
      val moduleClass   = toolLoader.loadClass("lazyvalgrade.jar.JarProcessor$")
      val module        = moduleClass.getField("MODULE$").get(null)
      val processMethod = moduleClass.getMethod("process", classOf[NioPath], classOf[NioPath])
      val result        = processMethod.invoke(module, input.toNIO, output.toNIO)
      val failedClasses = result.getClass.getMethod("failedClasses").invoke(result)
        .asInstanceOf[Int]
      if failedClasses > 0 then
        os.remove.all(output / os.up)
        Left(new BuildException(s"Failed to patch lazy vals in $input") {})
      else Right(())
    }
    catch {
      case e: Throwable =>
        os.remove.all(output / os.up)
        Left(new BuildException(
          s"Failed to patch lazy vals in $input: ${e.getMessage}",
          cause = e
        ) {})
    }

  private def sha1(path: Path): String = {
    val md     = MessageDigest.getInstance("SHA-1")
    val buffer = new Array[Byte](8192)
    val stream = os.read.inputStream(path)
    try
      var read = stream.read(buffer)
      while read >= 0 do
        md.update(buffer, 0, read)
        read = stream.read(buffer)
    finally stream.close()
    md.digest().map("%02x".format(_)).mkString
  }
}
