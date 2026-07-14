package scala.build.postprocessing

import os.Path
import sloth.jar.JarProcessor

import java.io.{ByteArrayOutputStream, PrintStream}
import java.math.BigInteger
import java.security.MessageDigest
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.{Directories, Logger}
import scala.jdk.CollectionConverters.*
import scala.util.Using
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

  def patchJarFile(
    jar: Path,
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Path] =
    if options.notForBloopOptions.sloth then
      Right(captureStdio(logger) {
        if jar.ext == "jar" then patchJar(jar, logger)
        else jar
      })
    else Right(jar)

  def patchByteCodeZipEntries(
    entries: Seq[(ZipEntry, Array[Byte])],
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[(ZipEntry, Array[Byte])]] =
    if !options.notForBloopOptions.sloth || entries.isEmpty then Right(entries)
    else
      val tmpJar = os.temp(prefix = "sloth-entries-", suffix = ".jar", dir = os.temp.dir())
      writeZipEntries(tmpJar, entries)
      patchJarFile(tmpJar, options, logger).map(readZipEntries)

  private def writeZipEntries(path: Path, entries: Seq[(ZipEntry, Array[Byte])]): Unit =
    val out = new ZipOutputStream(os.write.outputStream(path))
    try
      entries.foreach { (entry, content) =>
        out.putNextEntry(entry)
        out.write(content)
        out.closeEntry()
      }
    finally out.close()

  private def readZipEntries(path: Path): Seq[(ZipEntry, Array[Byte])] =
    val zf = new ZipFile(path.toIO)
    try
      zf.entries().asScala.toSeq.map { entry =>
        val content = Using.resource(zf.getInputStream(entry))(_.readAllBytes())
        (entry, content)
      }
    finally zf.close()

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
