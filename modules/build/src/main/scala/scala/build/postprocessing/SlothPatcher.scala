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
import scala.build.internal.util.WarningMessages
import scala.build.internal.{Constants, JarManifests}
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
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

  /** In-process memo of class directories covered by [[patchClassDirInPlace]] in this JVM.
    * Consulted by callers (e.g. `copyOutput`) to skip a redundant second pass over the same bytes;
    * does not short-circuit [[patchClassDirInPlace]] itself (needed under `--watch`).
    */
  private val patchedClassDirsInThisProcess =
    java.util.concurrent.ConcurrentHashMap.newKeySet[os.Path]()

  def wasPatchedInThisProcess(dir: os.Path): Boolean =
    patchedClassDirsInThisProcess.contains(dir)

  /** Patch `.class` files under `dir` in place when `--sloth` is enabled and `shouldPatch` is true.
    * Registers `dir` in the in-process memo once the pass completes (including when nothing needed
    * patching), so callers can skip re-patching a copy of the same directory.
    */
  def patchClassDirInPlace(
    dir: os.Path,
    options: BuildOptions,
    logger: Logger,
    shouldPatch: Boolean
  ): Either[BuildException, Unit] =
    if !options.notForBloopOptions.sloth || !shouldPatch || !os.isDir(dir) then Right(())
    else
      Right:
        captureStdio(logger):
          try
            withOriginalFallback(dir.toString, (), logger):
              val tmpInput =
                os.temp(prefix = "sloth-inplace-", suffix = ".jar", deleteOnExit = false)
              val tmpOutput =
                os.temp(prefix = "sloth-inplace-out-", suffix = ".jar", deleteOnExit = false)
              try
                jarDirectory(dir, tmpInput)
                runJarProcessor(tmpInput, tmpOutput) match
                  case Left(errorMsg) =>
                    logger.message(
                      s"$warnPrefix ${WarningMessages.slothCouldNotPatch(dir.toString, errorMsg)}"
                    )
                  case Right(result) if result.patchedClasses == 0 =>
                    logger.debug(s"No lazy vals to patch in place in $dir")
                  case Right(_) =>
                    writeBackPatchedClasses(dir, tmpInput, tmpOutput, logger)
                    logger.debug(s"Patched lazy vals in place in $dir")
              finally
                if os.exists(tmpInput) then os.remove(tmpInput)
                if os.exists(tmpOutput) then os.remove(tmpOutput)
          finally
            patchedClassDirsInThisProcess.add(dir)

  private def writeBackPatchedClasses(
    dir: os.Path,
    originalJar: os.Path,
    patchedJar: os.Path,
    logger: Logger
  ): Unit =
    val originalEntries = readZipEntries(originalJar).map((e, b) => e.getName -> b).toMap
    for (entry, content) <- readZipEntries(patchedJar) do
      val name = entry.getName
      if name.endsWith(".class") && !JarManifests.isManifestEntry(name) then
        val original = originalEntries.get(name)
        if !original.exists(java.util.Arrays.equals(_, content)) then
          val dest = dir / os.RelPath(name)
          os.write.over(dest, content, createFolders = true)
          logger.debug(s"Wrote patched class $name -> $dest")

  def patchByteCodeZipEntries(
    entries: Seq[(ZipEntry, Array[Byte])],
    options: BuildOptions,
    logger: Logger
  ): Either[BuildException, Seq[(ZipEntry, Array[Byte])]] =
    if !options.notForBloopOptions.sloth || entries.isEmpty then Right(entries)
    else
      val tmpJar = os.temp(prefix = "sloth-entries-", suffix = ".jar", deleteOnExit = false)
      try
        withOriginalFallback("bytecode zip entries", Right(entries), logger):
          writeZipEntries(tmpJar, entries)
          patchJarFile(tmpJar, options, logger).map(readZipEntries)
      finally if os.exists(tmpJar) then os.remove(tmpJar)

  /** ZIP local-file / empty-archive / spanned signatures. */
  private val zipLocalFileHeader: Array[Byte] = Array(0x50, 0x4b, 0x03, 0x04)
  private val zipEmptyArchive: Array[Byte]    = Array(0x50, 0x4b, 0x05, 0x06)
  private val zipSpannedArchive: Array[Byte]  = Array(0x50, 0x4b, 0x07, 0x08)
  private val zipEocdSignature: Array[Byte]   = Array(0x50, 0x4b, 0x05, 0x06)
  // EOCD is 22 bytes + up to 65535-byte comment
  private val eocdScanMax: Int = 22 + 65535

  /** Returns the byte offset where ZIP content begins, or None if the path is not a JAR-like
    * archive. Offset 0 means a plain archive; a positive offset means a launcher preamble precedes
    * the ZIP payload.
    */
  private[build] def zipStartOffset(path: os.Path): Option[Long] =
    if !os.isFile(path) then None
    else
      val size = os.size(path)
      if size < 4 then None
      else
        val header = os.read.bytes(path, offset = 0, count = 4)
        if isZipSignature(header) then Some(0L)
        else findPreambleZipOffset(path, size)

  private def isZipSignature(bytes: Array[Byte]): Boolean =
    bytes.length >= 4 && (
      bytes.startsWith(zipLocalFileHeader) ||
      bytes.startsWith(zipEmptyArchive) ||
      bytes.startsWith(zipSpannedArchive)
    )

  private def findPreambleZipOffset(path: os.Path, size: Long): Option[Long] =
    val scanLen = math.min(size, eocdScanMax.toLong).toInt
    if scanLen < 22 then None
    else
      val tail       = os.read.bytes(path, offset = size - scanLen, count = scanLen)
      val eocdInTail = findEocdOffset(tail)
      eocdInTail.flatMap { eocdRel =>
        val eocdPos        = size - scanLen + eocdRel
        val centralDirSize = readLittleEndianInt(tail, eocdRel + 12)
        val centralDirOff  = readLittleEndianInt(tail, eocdRel + 16)
        val preambleLength = eocdPos - centralDirSize - centralDirOff
        if preambleLength < 0 || preambleLength >= size then None
        else if preambleLength == 0 then Some(0L)
        else
          val atOffset = os.read.bytes(path, offset = preambleLength, count = 4)
          if isZipSignature(atOffset) ||
            (centralDirSize == 0 && centralDirOff == 0 && atOffset.startsWith(zipEmptyArchive))
          then Some(preambleLength)
          else None
      }

  private def findEocdOffset(bytes: Array[Byte]): Option[Int] =
    @annotation.tailrec
    def loop(i: Int): Option[Int] =
      if i < 0 then None
      else if bytes(i) == zipEocdSignature(0) &&
        bytes(i + 1) == zipEocdSignature(1) &&
        bytes(i + 2) == zipEocdSignature(2) &&
        bytes(i + 3) == zipEocdSignature(3)
      then Some(i)
      else loop(i - 1)
    loop(bytes.length - 22)

  private def readLittleEndianInt(bytes: Array[Byte], offset: Int): Long =
    (bytes(offset) & 0xff).toLong |
      ((bytes(offset + 1) & 0xff).toLong << 8) |
      ((bytes(offset + 2) & 0xff).toLong << 16) |
      ((bytes(offset + 3) & 0xff).toLong << 24)

  private def patchIfJar(path: os.Path, logger: Logger): os.Path =
    path.orOriginalOnFailure(logger):
      zipStartOffset(path) match
        case Some(offset) =>
          patchJar(path, offset, logger)
        case None =>
          if os.isFile(path) then
            logger.message(s"$warnPrefix ${WarningMessages.slothNotAnArchive(path)}")
          else
            logger.debug(s"Sloth skipping non-archive path: $path")
          path

  private def patchClassPathEntry(
    path: os.Path,
    patchProjectClassDirs: Boolean,
    logger: Logger
  ): os.Path =
    path.orOriginalOnFailure(logger):
      zipStartOffset(path) match
        case Some(offset) =>
          patchJar(path, offset, logger)
        case None if patchProjectClassDirs && os.isDir(path) =>
          patchClassDir(path, logger)
        case None =>
          logger.debug(s"Sloth skipping classpath entry: $path")
          path

  private def patchClassDir(dir: os.Path, logger: Logger): os.Path =
    dir.orOriginalOnFailure(logger):
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
          val jarred: Either[String, Unit] =
            try
              jarDirectory(dir, tmpInput)
              Right(())
            catch
              case NonFatal(e) => Left(e.getMessage)
          jarred match
            case Left(errorMsg) =>
              logger.message(
                s"$warnPrefix ${WarningMessages.slothCouldNotPatch(dir.toString, errorMsg)}"
              )
              dir
            case Right(()) =>
              publishCached(
                cachedDir,
                cached,
                out => runJarProcessor(tmpInput, out).map(_ => ())
              ) match
                case Right(cachedPath) =>
                  logger.debug(s"Patched lazy vals in class directory $dir -> $cachedPath")
                  cachedPath
                case Left(errorMsg) =>
                  logger.message(
                    s"$warnPrefix ${WarningMessages.slothCouldNotPatch(dir.toString, errorMsg)}"
                  )
                  dir
        finally if os.exists(tmpInput) then os.remove(tmpInput)

  private def jarDirectory(dir: os.Path, dest: os.Path): Unit =
    val manifest = JarManifests.merged(JarManifests.userManifestIn(dir), Nil)
    Using.resource(JarOutputStream(os.write.outputStream(dest), manifest)): jos =>
      for
        path <- os.walk(dir)
        if os.isFile(path)
      do
        val relativePath = path.relativeTo(dir).toString.replace('\\', '/')
        if !JarManifests.isManifestEntry(relativePath) then
          val entry = ZipEntry(relativePath)
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

  private def withOriginalFallback[T](subject: String, original: => T, logger: Logger)(f: => T): T =
    try f
    catch
      case NonFatal(e) =>
        logger.message(
          s"$warnPrefix ${WarningMessages.slothCouldNotPatch(
              subject,
              Option(e.getMessage).getOrElse(e.toString)
            )}"
        )
        original

  extension (path: os.Path)
    private def orOriginalOnFailure(logger: Logger)(patch: => os.Path): os.Path =
      withOriginalFallback(path.toString, path, logger)(patch)

  private def patchJar(jar: os.Path, zipOffset: Long, logger: Logger): os.Path =
    jar.orOriginalOnFailure(logger):
      val jarHash         = sha1(jar)
      val cachedDir       = cacheDir / Constants.slothVersion / jarHash
      val cached          = cachedDir / jar.last
      val unpatchedMarker = cachedDir / unpatchedMarkerName
      val signed          = isSigned(jar)

      if os.exists(cached) then
        if signed then
          logger.message(s"$warnPrefix ${WarningMessages.slothStrippedJarSignatures(jar)}")
        cached
      else if os.exists(unpatchedMarker) then
        jar
      else
        os.makeDir.all(cachedDir)
        val payloadJar =
          if zipOffset == 0 then jar
          else
            val extracted =
              os.temp(
                prefix = "sloth-payload-",
                suffix = ".jar",
                dir = cachedDir,
                deleteOnExit = false
              )
            extractZipPayload(jar, zipOffset, extracted)
            extracted
        val tmpOutput =
          os.temp(prefix = "sloth-patch-", suffix = ".tmp", dir = cachedDir, deleteOnExit = false)
        try
          runJarProcessor(payloadJar, tmpOutput) match
            case Left(message) =>
              logger.message(
                s"$warnPrefix ${WarningMessages.slothCouldNotPatch(jar.toString, message)}"
              )
              jar
            case Right(result) =>
              if result.patchedClasses == 0 then
                os.write(unpatchedMarker, "", createFolders = true)
                logger.debug(s"No lazy vals to patch in $jar")
                jar
              else
                val patchedPayload =
                  if signed then
                    val stripped = os.temp(
                      prefix = "sloth-stripped-",
                      suffix = ".tmp",
                      dir = cachedDir,
                      deleteOnExit = false
                    )
                    try
                      stripSignatures(tmpOutput, stripped)
                      stripped
                    catch
                      case NonFatal(e) =>
                        if os.exists(stripped) then os.remove(stripped)
                        throw e
                  else tmpOutput
                try
                  publishPatchedArchive(jar, zipOffset, patchedPayload, cached, signed, logger)
                finally
                  if patchedPayload != tmpOutput && os.exists(patchedPayload) then
                    os.remove(patchedPayload)
        finally
          if os.exists(tmpOutput) then os.remove(tmpOutput)
          if payloadJar != jar && os.exists(payloadJar) then os.remove(payloadJar)

  private def extractZipPayload(jar: os.Path, zipOffset: Long, dest: os.Path): Unit =
    val size   = os.size(jar)
    val length = (size - zipOffset).toInt
    val bytes  = os.read.bytes(jar, offset = zipOffset, count = length)
    os.write.over(dest, bytes, createFolders = true)

  private def publishPatchedArchive(
    jar: os.Path,
    zipOffset: Long,
    patchedPayload: os.Path,
    cached: os.Path,
    signed: Boolean,
    logger: Logger
  ): os.Path =
    val cachedDir = cached / os.up
    val assembled =
      if zipOffset == 0 then patchedPayload
      else
        val withPreamble = os.temp(
          prefix = "sloth-preamble-",
          suffix = ".tmp",
          dir = cachedDir,
          deleteOnExit = false
        )
        val preamble = os.read.bytes(jar, offset = 0, count = zipOffset.toInt)
        os.write.over(withPreamble, preamble ++ os.read.bytes(patchedPayload), createFolders = true)
        withPreamble
    try
      try
        os.move(assembled, cached, atomicMove = true, replaceExisting = false)
      catch
        case _: FileAlreadyExistsException | _: AtomicMoveNotSupportedException =>
          try os.move(assembled, cached, replaceExisting = false)
          catch case _: FileAlreadyExistsException => ()
      copyPermissions(jar, cached)
      if signed then
        logger.message(s"$warnPrefix ${WarningMessages.slothStrippedJarSignatures(jar)}")
      logger.debug(s"Patched lazy vals in $jar -> $cached")
      cached
    finally
      if assembled != patchedPayload && os.exists(assembled) then os.remove(assembled)

  private def copyPermissions(from: os.Path, to: os.Path): Unit =
    try os.perms.set(to, os.perms(from))
    catch case NonFatal(_) => ()

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
