package scala.build.postprocessing

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

object SlothPatcher:

  private val cacheDir = Directories.directories.cacheDir / "sloth"

  def transformClassPath(
    classPath: Seq[os.Path],
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[os.Path]] =
    if options.notForBloopOptions.sloth then
      Right(captureStdio(logger)(classPath.map(patchIfJar(_, logger))))
    else Right(classPath)

  def patchJarFile(
    jar: os.Path,
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, os.Path] =
    if options.notForBloopOptions.sloth then
      Right(captureStdio(logger)(patchIfJar(jar, logger)))
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

  private def patchIfJar(path: os.Path, logger: Logger): os.Path =
    if path.ext == "jar" then patchJar(path, logger)
    else path

  private def writeZipEntries(path: os.Path, entries: Seq[(ZipEntry, Array[Byte])]): Unit =
    Using.resource(ZipOutputStream(os.write.outputStream(path))): out =>
      entries.foreach: (entry, content) =>
        out.putNextEntry(entry)
        out.write(content)
        out.closeEntry()

  private def readZipEntries(path: os.Path): Seq[(ZipEntry, Array[Byte])] =
    Using.resource(ZipFile(path.toIO)): zf =>
      zf.entries().asScala.toSeq.map: entry =>
        val content = Using.resource(zf.getInputStream(entry))(_.readAllBytes())
        (entry, content)

  private def captureStdio[T](logger: Logger)(f: => T): T =
    val outBuffer   = ByteArrayOutputStream()
    val errBuffer   = ByteArrayOutputStream()
    val originalOut = System.out
    val originalErr = System.err
    System.setOut(PrintStream(outBuffer, true))
    System.setErr(PrintStream(errBuffer, true))
    try f
    finally
      System.setOut(originalOut)
      System.setErr(originalErr)
      val captured = (outBuffer.toString ++ errBuffer.toString).trim
      if captured.nonEmpty then logger.debug(captured)

  private def patchJar(jar: os.Path, logger: Logger): os.Path =
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

  private def runJarProcessor(input: os.Path, output: os.Path): Either[String, Unit] =
    try
      val result = JarProcessor.process(input.toNIO, output.toNIO)
      if result.errors.nonEmpty then
        Left(
          s"Failed to patch lazy vals in $input (${result.failedClasses} failed classes): ${result.errors.mkString("; ")}"
        )
      else Right(())
    catch
      case NonFatal(e) =>
        Left(s"Failed to patch lazy vals in $input: ${e.getMessage}")

  private def sha1(path: os.Path): String =
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(path))
    String.format("%040x", BigInteger(1, md.digest()))
