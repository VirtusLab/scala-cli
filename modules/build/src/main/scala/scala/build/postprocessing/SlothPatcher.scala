package scala.build.postprocessing

import os.Path
import sloth.jar.JarProcessor

import java.io.{ByteArrayOutputStream, PrintStream}
import java.math.BigInteger
import java.security.MessageDigest

import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.{Directories, Logger}
import scala.util.control.NonFatal

object SlothPatcher {

  private val cacheDir = Directories.directories.cacheDir / "sloth"

  def transformClassPath(
    classPath: Seq[Path],
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[Path]] =
    if options.notForBloopOptions.sloth then
      Right {
        // sloth logs to stdout/stderr via its own bundled logging; capture it so it
        // doesn't pollute the user program's output, routing anything it prints to debug.
        captureStdio(logger) {
          classPath.map { entry =>
            if entry.ext == "jar" then patchJar(entry, logger)
            else entry
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

  private def patchJar(jar: Path, logger: Logger): Path =
    val cachedDir = cacheDir / Constants.slothVersion / sha1(jar)
    val cached    = cachedDir / jar.last
    if os.exists(cached) then cached
    else
      os.makeDir.all(cachedDir)
      runJarProcessor(jar, cached) match
        case Right(()) =>
          logger.debug(s"Patched lazy vals in $jar -> $cached")
          cached
        case Left(message) =>
          os.remove.all(cachedDir)
          logger.message(s"Could not patch lazy vals in $jar, using original: $message")
          jar

  private def runJarProcessor(
    input: Path,
    output: Path
  ): Either[String, Unit] =
    try
      val result = JarProcessor.process(input.toNIO, output.toNIO)
      if result.failedClasses > 0 then
        val details = if result.errors.nonEmpty then s": ${result.errors.mkString("; ")}" else ""
        Left(s"Failed to patch lazy vals in $input ($result.failedClasses failed classes)$details")
      else Right(())
    catch
      case NonFatal(e) =>
        Left(s"Failed to patch lazy vals in $input: ${e.getMessage}")

  private def sha1(path: Path): String =
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(path))
    String.format("%040x", new BigInteger(1, md.digest()))
}
