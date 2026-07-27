package scala.build.postprocessing

import sloth.jar.JarProcessor

import java.io.{ByteArrayOutputStream, PrintStream}
import java.math.BigInteger
import java.nio.file.attribute.FileTime
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException}
import java.security.MessageDigest
import java.util.jar.{Attributes as JarAttributes, JarOutputStream, Manifest as JarManifest}
import java.util.zip.{ZipEntry, ZipFile, ZipOutputStream}

import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.internal.util.WarningMessages
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
      val tmpJar = os.temp(prefix = "sloth-entries-", suffix = ".jar", deleteOnExit = false)
      try
        writeZipEntries(tmpJar, entries)
        patchJarFile(tmpJar, options, logger).map(readZipEntries)
      finally os.remove(tmpJar)

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
      val tmpInput = os.temp(
        prefix = "sloth-classdir-",
        suffix = ".jar",
        dir = cachedDir,
        deleteOnExit = false
      )
      try
        jarDirectory(dir, tmpInput)
        publishCached(cachedDir, cached, out => runJarProcessor(tmpInput, out).map(_ => ())) match
          case Right(cachedPath) =>
            logger.debug(s"Patched lazy vals in class directory $dir -> $cachedPath")
            cachedPath
          case Left(errorMsg) =>
            logger.message(s"Could not patch lazy vals in $dir, using original: $errorMsg")
            dir
      finally if os.exists(tmpInput) then os.remove(tmpInput)

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

  // --- Signature detection and stripping ---

  private val signatureExtensions = Set("sf", "dsa", "rsa", "ec")

  /** Returns entry names of signature files in a JAR (META-INF entries: .SF, .DSA, .RSA, .EC,
    * SIG-).
    */
  private[build] def signatureEntryNames(jar: os.Path): Seq[String] =
    Using.resource(ZipFile(jar.toIO)): zf =>
      zf.entries().asScala.toSeq
        .map(_.getName)
        .filter: name =>
          if !name.startsWith("META-INF/") then false
          else
            val relativeName = name.stripPrefix("META-INF/")
            !relativeName.contains("/") && (
              signatureExtensions.contains(
                relativeName.toLowerCase.split("\\.").lastOption.getOrElse("")
              ) ||
              relativeName.toUpperCase.startsWith("SIG-")
            )

  /** Checks if a JAR contains signature files. */
  private[build] def isSigned(jar: os.Path): Boolean =
    signatureEntryNames(jar).nonEmpty

  /** Strips signature files and digest attributes from a JAR. */
  private[build] def stripSignatures(input: os.Path, output: os.Path): Unit =
    val sigEntries = signatureEntryNames(input).toSet
    Using.resource(ZipFile(input.toIO)): zf =>
      val manifestEntry    = zf.getEntry("META-INF/MANIFEST.MF")
      val originalManifest =
        if manifestEntry != null then
          Using.resource(zf.getInputStream(manifestEntry))(is => JarManifest(is))
        else
          val m = JarManifest()
          m.getMainAttributes.put(JarAttributes.Name.MANIFEST_VERSION, "1.0")
          m

      val newManifest = JarManifest()
      newManifest.getMainAttributes.putAll(originalManifest.getMainAttributes)

      for (entryName, attrs) <- originalManifest.getEntries.asScala do
        val newAttrs = JarAttributes()
        for (key, value) <- attrs.asScala do
          val keyName = key.toString
          if !keyName.toLowerCase.contains("-digest") then
            newAttrs.put(key, value)
        if !newAttrs.isEmpty then
          newManifest.getEntries.put(entryName, newAttrs)

      Using.resource(JarOutputStream(os.write.outputStream(output), newManifest)): jos =>
        for entry <- zf.entries().asScala do
          val name = entry.getName
          if !sigEntries.contains(name) && name != "META-INF/MANIFEST.MF" then
            val newEntry = ZipEntry(name)
            if entry.getTime >= 0 then
              newEntry.setLastModifiedTime(FileTime.fromMillis(entry.getTime))
            val content = Using.resource(zf.getInputStream(entry))(_.readAllBytes())
            newEntry.setSize(content.length)
            jos.putNextEntry(newEntry)
            jos.write(content)
            jos.closeEntry()

  // --- End signature handling ---

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

  private val unpatchedMarkerName = ".sloth-unpatched"

  private def patchJar(jar: os.Path, logger: Logger): os.Path =
    val jarHash         = sha1(jar)
    val cachedDir       = cacheDir / Constants.slothVersion / jarHash
    val cached          = cachedDir / jar.last
    val unpatchedMarker = cachedDir / unpatchedMarkerName
    val signed          = isSigned(jar)

    if os.exists(cached) then
      if signed then
        logger.message(WarningMessages.slothStrippedJarSignatures(jar))
      cached
    else if os.exists(unpatchedMarker) then
      jar
    else
      os.makeDir.all(cachedDir)
      val tmpOutput =
        os.temp(prefix = "sloth-patch-", suffix = ".tmp", dir = cachedDir, deleteOnExit = false)
      try
        runJarProcessor(jar, tmpOutput) match
          case Left(message) =>
            logger.message(s"Could not patch lazy vals in $jar, using original: $message")
            jar
          case Right(result) =>
            if result.patchedClasses == 0 then
              os.write(unpatchedMarker, "", createFolders = true)
              logger.debug(s"No lazy vals to patch in $jar")
              jar
            else if signed then
              val stripped = os.temp(
                prefix = "sloth-stripped-",
                suffix = ".tmp",
                dir = cachedDir,
                deleteOnExit = false
              )
              try
                stripSignatures(tmpOutput, stripped)
                try
                  os.move(stripped, cached, atomicMove = true, replaceExisting = false)
                catch
                  case _: FileAlreadyExistsException | _: AtomicMoveNotSupportedException =>
                    try os.move(stripped, cached, replaceExisting = false)
                    catch case _: FileAlreadyExistsException => ()
                logger.message(WarningMessages.slothStrippedJarSignatures(jar))
                logger.debug(s"Patched lazy vals in $jar -> $cached")
                cached
              finally
                if os.exists(stripped) then os.remove(stripped)
            else
              try
                os.move(tmpOutput, cached, atomicMove = true, replaceExisting = false)
              catch
                case _: FileAlreadyExistsException | _: AtomicMoveNotSupportedException =>
                  try os.move(tmpOutput, cached, replaceExisting = false)
                  catch case _: FileAlreadyExistsException => ()
              logger.debug(s"Patched lazy vals in $jar -> $cached")
              cached
      finally if os.exists(tmpOutput) then os.remove(tmpOutput)

  /** Write to a unique temp file in `cachedDir`, then atomically move into `cached`. */
  private def publishCached(
    cachedDir: os.Path,
    cached: os.Path,
    writeTo: os.Path => Either[String, Unit]
  ): Either[String, os.Path] =
    os.makeDir.all(cachedDir)
    if os.exists(cached) then Right(cached)
    else
      val tmp =
        os.temp(prefix = "sloth-patch-", suffix = ".tmp", dir = cachedDir, deleteOnExit = false)
      try
        writeTo(tmp) match
          case Left(message) =>
            Left(message)
          case Right(()) =>
            try
              os.move(tmp, cached, atomicMove = true, replaceExisting = false)
              Right(cached)
            catch
              case _: FileAlreadyExistsException =>
                // Another process won the race; use their completed cache entry.
                Right(cached)
              case _: AtomicMoveNotSupportedException =>
                try
                  os.move(tmp, cached, replaceExisting = false)
                  Right(cached)
                catch
                  case _: FileAlreadyExistsException => Right(cached)
      finally if os.exists(tmp) then os.remove(tmp)

  private def runJarProcessor(
    input: os.Path,
    output: os.Path
  ): Either[String, JarProcessor.JarResult] =
    try
      val result = JarProcessor.process(input.toNIO, output.toNIO)
      if result.errors.nonEmpty then
        Left(
          s"Failed to patch lazy vals in $input (${result.failedClasses} failed classes): ${result.errors.mkString("; ")}"
        )
      else Right(result)
    catch
      case NonFatal(e) =>
        Left(s"Failed to patch lazy vals in $input: ${e.getMessage}")

  private def sha1(path: os.Path): String =
    val md = MessageDigest.getInstance("SHA-1")
    md.update(os.read.bytes(path))
    String.format("%040x", BigInteger(1, md.digest()))
