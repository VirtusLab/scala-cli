package scala.build.postprocessing

import sloth.jar.JarProcessor

import java.io.{ByteArrayOutputStream, PrintStream}
import java.math.BigInteger
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.jar.{Attributes as JarAttributes, JarOutputStream, Manifest as JarManifest}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.build.{Build, Directories, Logger, coursierVersion, isScala38OrNewer}
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NonFatal

object SlothPatcher:

  private val cacheDir = Directories.directories.cacheDir / "sloth"

  private[build] def shouldPatchProjectClasses(
    hasJava: Boolean,
    hasScala: Boolean,
    scalaVersions: Seq[String]
  ): Boolean =
    val isPureJavaProject = hasJava && !hasScala
    !isPureJavaProject &&
    scalaVersions.exists(v => !v.coursierVersion.isScala38OrNewer)

  def shouldPatchProjectClasses(builds: Seq[Build.Successful]): Boolean =
    shouldPatchProjectClasses(
      hasJava = builds.exists(_.sources.hasJava),
      hasScala = builds.exists(_.sources.hasScala),
      scalaVersions = builds.flatMap(_.scalaParams).map(_.scalaVersion)
    )

  def transformClassPath(
    classPath: Seq[os.Path],
    options: BuildOptions,
    logger: Logger,
    patchProjectClassDirs: Boolean = false
  ): Either[BuildException, Seq[os.Path]] =
    if options.notForBloopOptions.sloth then
      Right(captureStdio(logger)(classPath.map(patchClassPathEntry(
        _,
        patchProjectClassDirs,
        logger
      ))))
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

  private def patchClassPathEntry(
    path: os.Path,
    patchProjectClassDirs: Boolean,
    logger: Logger
  ): os.Path =
    if path.ext == "jar" then patchJar(path, logger)
    else if patchProjectClassDirs && os.isDir(path) then patchClassDir(path, logger)
    else path

  private def patchClassDir(dir: os.Path, logger: Logger): os.Path =
    val dirHash   = sha1OfDir(dir)
    val cachedDir = cacheDir / Constants.slothVersion / "dirs" / dirHash
    val cached    = cachedDir / s"${dir.last}.jar"
    if os.exists(cached) then cached
    else
      os.makeDir.all(cachedDir)
      val tmpJar = os.temp(prefix = "sloth-classdir-", suffix = ".jar", dir = cachedDir)
      jarDirectory(dir, tmpJar)
      runJarProcessor(tmpJar, cached) match
        case Right(()) =>
          os.remove(tmpJar)
          logger.debug(s"Patched lazy vals in class directory $dir -> $cached")
          cached
        case Left(message) =>
          os.remove.all(cachedDir)
          logger.message(s"Could not patch lazy vals in $dir, using original: $message")
          dir

  private def jarDirectory(dir: os.Path, dest: os.Path): Unit =
    val manifest = JarManifest()
    manifest.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
    Using.resource(JarOutputStream(os.write.outputStream(dest), manifest)): jos =>
      for
        path <- os.walk(dir)
        if os.isFile(path)
      do
        val relativePath = path.relativeTo(dir).toString.replace('\\', '/')
        val entry        = ZipEntry(relativePath)
        entry.setLastModifiedTime(FileTime.fromMillis(os.mtime(path)))
        val content = os.read.bytes(path)
        entry.setSize(content.length)
        jos.putNextEntry(entry)
        jos.write(content)
        jos.closeEntry()

  private def sha1OfDir(dir: os.Path): String =
    val md    = MessageDigest.getInstance("SHA-1")
    val files = os.walk(dir).filter(os.isFile).sorted
    for file <- files do
      val relativePath = file.relativeTo(dir).toString
      md.update(relativePath.getBytes("UTF-8"))
      md.update(os.read.bytes(file))
    String.format("%040x", BigInteger(1, md.digest()))

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

  // System.out/err are process-global; sloth's BytecodePatcher prints stack
  // traces directly to System.err. We must swap the global streams to capture
  // that noise, so the whole swap/restore window is serialized behind this lock
  // to stay correct under concurrent patching (e.g. --watch reruns).
  private val stdioLock = new Object

  private[build] def captureStdio[T](logger: Logger)(f: => T): T =
    stdioLock.synchronized:
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
