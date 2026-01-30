package scala.build.internals
import java.util.Locale

import scala.annotation.tailrec
import scala.build.Logger
import scala.collection.immutable.TreeMap
import scala.io.Source
import scala.util.Using

/*
 * Directly invoke `native-image.exe`:
 *   run `vcvarsall.bat` **only to capture environment**
 *   merge MSVC environment and base environments.
 *   directly spawn native-image.exe
 * Avoids problematic Ctrl-C behavior of .bat / .cmd files.
 */

object MsvcEnvironment {

  // Lower threshold to ensure native-image's internal paths (which can add 100-130+ chars
  // for deeply nested source files) don't exceed Windows 260-char MAX_PATH limit.
  // Native-image creates paths like: native-sources\graal\com\oracle\svm\...\Target_ClassName.c
  private val pathLengthLimit = 90

  /*
   * Call `native-image.exe` with captured vcvarsall.bat environment.
   * @return process exit code.
   */
  def msvcNativeImageProcess(
    command: Seq[String],
    workingDir: os.Path,
    logger: Logger
  ): Int = {
    // Use shortened working dir when path is too long; otherwise vcvars/native-image run with
    // long cwd and GraalVM's "automatically set up Windows build environment" hits 260-char limit.
    val (actualWorkingDir, driveToUnalias) =
      if (workingDir.toString.length >= pathLengthLimit) {
        val (driveLetter, shortPath) = getShortenedPath(workingDir, logger)
        (shortPath, Some(driveLetter))
      }
      else
        (workingDir, None)

    try {
      val vcvOpt = vcvarsOpt(logger)
      vcvOpt match {
        case None =>
          logger.debug(s"not found: vcvars64.bat")
          -1
        case Some(vcvars) =>
          logger.debug(s"Using vcvars script $vcvars")

          val msvcEnv: Map[String, String] = captureVcvarsEnv(vcvars, actualWorkingDir, logger)

          // Validate that critical MSVC variables were captured
          // VSINSTALLDIR is what GraalVM native-image checks to detect pre-configured MSVC
          val requiredVars =
            Seq("VSINSTALLDIR", "VCINSTALLDIR", "VCToolsInstallDir", "INCLUDE", "LIB")
          val missingVars = requiredVars.filterNot(msvcEnv.contains)

          if msvcEnv.isEmpty then
            logger.error("MSVC environment capture failed - no environment variables captured")
            logger.error("Please ensure Visual Studio 2022 with C++ build tools is installed")
            logger.error(s"vcvars script used: $vcvars")
            logger.error(s"working directory: $actualWorkingDir")
            -1
          else if missingVars.nonEmpty then
            logger.error(s"MSVC environment incomplete - missing: ${missingVars.mkString(", ")}")
            logger.error(
              "Please ensure Visual Studio 2022 with C++ build tools is properly installed"
            )
            logger.error(s"vcvars script used: $vcvars")
            logger.error(s"Captured environment has ${msvcEnv.size} variables")
            -1
          else
            // show aliased drive map
            getSubstMappings.foreach((k, v) => logger.message(s"substMap  $k: -> $v"))

            val finalEnv =
              msvcEnv +
                ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

            logger.debug(s"msvc PATH entries:")
            finalEnv.getOrElse("PATH", "").split(";").toSeq.foreach { entry =>
              logger.debug(s"$entry;")
            }
            Seq(
              "VCToolsInstallDir",
              "VCToolsVersion",
              "VCINSTALLDIR",
              "WindowsSdkDir",
              "WindowsSdkVersion",
              "INCLUDE",
              "LIB",
              "LIBPATH"
            ).foreach { key =>
              logger.debug(s"""$key=${msvcEnv.getOrElse(key, "<missing>")}""")
            }

            // Replace native-image.cmd with native-image.exe, if applicable
            val updatedCommand: Seq[String] =
              command.headOption match {
                case Some(cmd) if cmd.toLowerCase.endsWith("native-image.cmd") =>
                  val cmdPath   = os.Path(cmd, os.pwd)
                  val graalHome = cmdPath / os.up / os.up
                  resolveNativeImage(graalHome) match {
                    case Some(exe) =>
                      exe.toString +: command.tail
                    case None =>
                      command // fall back to the .cmd wrapper
                  }
                case _ =>
                  command
              }

            logger.debug(s"native-image w/args: $updatedCommand")

            val result =
              os.proc(updatedCommand)
                .call(
                  cwd = actualWorkingDir,
                  env = finalEnv,
                  stdout = os.Inherit,
                  stderr = os.Inherit
                )

            result.exitCode
      }
    }
    finally
      driveToUnalias.foreach(unaliasDriveLetter)
  }

  // =========================
  // Capture MSVC environment
  // =========================
  private def captureVcvarsEnv(
    vcvars: os.Path,
    workingDir: os.Path,
    logger: Logger
  ): Map[String, String] = {

    val vcvarsCmd = vcvars.toIO.getAbsolutePath

    val sentinel = "vcvSentinel_7f4a2b"
    val cmd      = Seq(
      cmdExe,
      "/c",
      s"""set "VSCMD_DEBUG=1" & call "$vcvarsCmd" & echo $sentinel & where cl & where link & where lib & cl /Bv & set"""
    )

    val out = new StringBuilder
    val err = new StringBuilder

    val res = os.proc(cmd).call(
      cwd = workingDir,
      env = sys.env,
      stdout = os.ProcessOutput.Readlines(line => out.append(line).append("\n")),
      stderr = os.ProcessOutput.Readlines(line => err.append(line).append("\n")),
      check = false
    )

    if res.exitCode != 0 then
      logger.error(s"vcvars call failed with exit code ${res.exitCode}")
      Map.empty
    else
      def toVec(iter: Iterator[String]): Vector[String] =
        iter.map(_.trim).filter(_.nonEmpty).toVector
      val errlines = toVec(
        err.result().linesIterator
      ).filter(_ != "cl : Command line error D8003 : missing source filename")
      val outlines = toVec(out.result().linesIterator)

      // Split at sentinel
      val (debugLog, afterSentinel) = outlines.span(_ != sentinel)

      // Drop the sentinel itself
      val envLines   = afterSentinel.drop(1)
      val debugLines = errlines ++ debugLog

      given Ordering[String] =
        Ordering.by[String, String](_.toLowerCase(Locale.ROOT))(using Ordering.String)

      // Parse KEY=VALUE lines, preserving original key casing
      val envMap =
        TreeMap.empty[String, String] ++
          envLines.flatMap { line =>
            line.split("=", 2) match
              case Array(k, v) => Some(k -> v) // preserve original spelling
              case _           => None
          }

      if logger.verbosity > 0 then
        debugLines.foreach(dbg => logger.debug(s"$dbg"))
        debugLines.find(_.contains("Writing post-execution environment to ")) match {
          case None    =>
          case Some(s) =>
            val envMapFile = s.replaceFirst(".* environment to ", "")
            envReport(envMap, envMapFile, logger)
        }

      envMap
  }

  def envReport(captEnv: Map[String, String], logfile: String, logger: Logger): Unit = {
    import java.nio.file.{Files, Paths}
    import scala.jdk.CollectionConverters.*

    val path = Paths.get(logfile)

    if !Files.exists(path) then
      logger.message(s"not found: $logfile")
    else
      // Parse KEY=VALUE lines from the file
      val fileEnv: Map[String, String] =
        Files.readAllLines(path).asScala.flatMap { line =>
          line.split("=", 2) match
            case Array(k, v) => Some(k -> v)
            case _           => None
        }.toMap

      // Keys present in both but with different values
      val differingValues =
        for
          (k, v1) <- captEnv
          v2      <- fileEnv.get(k)
          if v1 != v2
        yield (k, v1, v2)

      // Keys only in captured env
      val onlyInCaptured =
        captEnv.keySet.diff(fileEnv.keySet).map(k => k -> captEnv(k))

      // Keys only in file env
      val onlyInFile =
        fileEnv.keySet.diff(captEnv.keySet).map(k => k -> fileEnv(k))

      if differingValues.nonEmpty then
        logger.debug("=== keys with different values ===")
        differingValues.foreach { case (k, v1, v2) =>
          logger.debug(s"$k:\n  captured = $v1\n  file     = $v2")
        }

      if onlyInCaptured.nonEmpty then
        logger.debug("=== only in captured env ===")
        onlyInCaptured.foreach { case (k, v) =>
          logger.debug(s"$k = $v")
        }

      if onlyInFile.nonEmpty then
        logger.debug("=== only in file env ===")
        onlyInFile.foreach { case (k, v) =>
          logger.debug(s"$k = $v")
        }

      if differingValues.isEmpty && onlyInCaptured.isEmpty && onlyInFile.isEmpty then
        logger.debug("envReport: no differences")
  }

  def getSubstMappings: Map[Char, String] =
    try
      val (exitCode, output) = execWindowsCmd(cmdExe, "/c", "subst")
      if exitCode != 0 then Map.empty
      else
        output
          .linesIterator
          .flatMap { line =>
            // Example: "X:\: => C:\path\to\something"
            val parts = line.split("=>").map(_.trim)
            if parts.length == 2 then
              val drivePart = parts(0) // "X:\:"
              val target    = parts(1) // "C:\path\to\something"

              // Extract the drive letter safely
              val maybeDrive: Option[Char] =
                if drivePart.length >= 2 && drivePart(1) == ':' then
                  Some(drivePart(0)) // 'X'
                else None

              maybeDrive.map(_ -> target)
            else None
          }
          .toMap
    catch
      case _: Throwable => Map.empty

  // Reduce duplicate drive aliases to no more than one;
  // @return Some(single-alias) or None.
  private def consolidateAliases(
    targetPath: os.Path,
    logger: Logger
  ): Option[Char] = {
    val mappings  = getSubstMappings
    val targetStr = targetPath.toString.toLowerCase

    // Find all drives pointing to our target (case-insensitive on Windows)
    val matchingDrives = mappings.filter { case (_, target) =>
      target.toLowerCase == targetStr
    }.keys.toList.sorted

    matchingDrives match {
      case Nil =>
        // No existing aliases for this target
        None

      case kept :: duples =>
        // Keep first one, remove the rest
        duples.foreach { drive =>
          logger.debug(s"Removing duplicate alias $drive: -> $targetStr")
          try
            unaliasDriveLetter(drive)
          catch {
            case e: Exception =>
              logger.debug(s"Failed to remove duplicate alias $drive: ${e.getMessage}")
          }
        }

        if (duples.isEmpty)
          logger.debug(s"Reusing existing alias $kept: -> $targetStr")
        else
          logger.debug(
            s"Consolidated ${duples.size + 1} aliases to $kept:, removed: ${duples.mkString(", ")}"
          )

        Some(kept)
    }
  }

  // Find or create a shortened alias for the given path
  def getShortenedPath(
    currentHome: os.Path,
    logger: Logger
  ): (Char, os.Path) = {
    val from = currentHome / os.up

    val driveLetter = consolidateAliases(from, logger) match {
      case Some(existingDrive) =>
        existingDrive // Reuse existing alias

      case None =>
        // Create new alias
        val driveLetter = availableDriveLetter()
        logger.debug(s"Creating drive alias $driveLetter: -> $from")
        aliasDriveLetter(driveLetter, from.toString)
        driveLetter
    }
    val drivePath = os.Path(s"$driveLetter:" + "\\")
    val newHome   = drivePath / currentHome.last
    (driveLetter, newHome)
  }

  private def availableDriveLetter(): Char = {
    // if a drive letter has already been mapped by SUBST, it isn't free
    val substDrives: Set[Char] = getSubstMappings.keySet
    @tailrec
    def helper(from: Char): Char =
      if (from > 'Z') sys.error("Cannot find free drive letter")
      else if (mountedDrives.contains(from) || substDrives.contains(from))
        helper((from + 1).toChar)
      else from

    helper('D')
  }

  def aliasDriveLetter(driveLetter: Char, from: String): Unit =
    execWindowsCmd(cmdExe, "/c", s"subst $driveLetter: \"$from\"")

  def unaliasDriveLetter(driveLetter: Char): Int =
    execWindowsCmd(cmdExe, "/c", s"subst $driveLetter: /d")._1

  def execWindowsCmd(cmd: String*): (Int, String) =
    val pb = new ProcessBuilder(cmd*)
    pb.redirectInput(ProcessBuilder.Redirect.INHERIT)
    pb.redirectError(ProcessBuilder.Redirect.INHERIT)
    pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
    val p = pb.start()
    // read stdout fully
    val output = Using(Source.fromInputStream(p.getInputStream, "UTF-8")) { source =>
      source.mkString
    }.getOrElse("")

    val exitCode = p.waitFor()
    (exitCode, output)

  def setCodePage(cp: String): Int =
    execWindowsCmd(cmdExe, "/c", s"chcp $cp")._1

  def getCodePage: String = {
    val out = execWindowsCmd(cmdExe, "/c", "chcp")._2
    out.split(":").lastOption.map(_.trim).getOrElse("") // Extract the number
  }

  def getCodePage(logger: Logger): String =
    try {
      val out = os.proc(cmdExe, "/c", "chcp").call().out.text().trim
      out.split(":").lastOption.map(_.trim).getOrElse("") // Extract the number
    }
    catch {
      case e: Exception =>
        logger.debug(s"unable to get initial code page: ${e.getMessage}")
        ""
    }

  private def resolveNativeImage(graalHome: os.Path): Option[os.Path] = {
    val candidates = Seq(
      graalHome / "lib" / "svm" / "bin" / "native-image.exe",
      graalHome / "bin" / "native-image.exe",
      graalHome / "native-image.exe"
    )
    candidates.find(os.exists)
  }

  private def vcvarsOpt(logger: Logger): Option[os.Path] = {
    val candidates =
      vcVarsCandidates
        .iterator
        .map(os.Path(_, os.pwd))
        .filter(os.exists(_))
        .toSeq

    if (candidates.isEmpty) None
    else {
      // Sort lexicographically; newest VS installs always sort last
      val sorted = candidates.sortBy(_.toString)
      sorted.foreach(s => logger.debug(s"candidate: $s"))
      sorted.lastOption
    }
  }

  // newest VS first, Enterprise > Community > BuildTools
  private def vcVersions                              = Seq("2022", "2019", "2017")
  private def vcEditions                              = Seq("Enterprise", "Community", "BuildTools")
  private lazy val vcVarsCandidates: Iterable[String] =
    EnvVar.Misc.vcVarsAll.valueOpt ++ {
      for {
        isX86   <- Seq(false, true)
        version <- vcVersions
        edition <- vcEditions
      } yield {
        val programFiles = if (isX86) "Program Files (x86)" else "Program Files"
        """C:\""" + programFiles + """\Microsoft Visual Studio\""" + version + "\\" + edition +
          """\VC\Auxiliary\Build\vcvars64.bat"""
      }
    }

  lazy val mountedDrives: String = {
    val str         = "HKEY_LOCAL_MACHINE/SYSTEM/MountedDevices".replace('/', '\\')
    val queryDrives = s"reg query $str"
    val lines       = os.proc(cmdExe, "/c", queryDrives).call().out.lines()
    val dosDevices  = lines.filter { s =>
      s.contains("DosDevices")
    }.map { s =>
      s.replaceAll(".DosDevices.", "").replaceAll(":.*", "")
    }
    dosDevices.mkString
  }

  lazy val systemRoot: String = sys.env.getOrElse("SystemRoot", "C:\\Windows").stripSuffix("\\")
  lazy val cmdExe: String     = s"$systemRoot\\System32\\cmd.exe"
}
