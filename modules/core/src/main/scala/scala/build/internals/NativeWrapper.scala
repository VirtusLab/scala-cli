package scala.build.internals

import scala.annotation.tailrec
import scala.build.Logger
import scala.io.Source
import scala.util.Using

/*
 * Invoke `native-image.exe` inside a vcvars-initialized cmd.exe session.
 *
 * A temp batch file calls `vcvars64.bat` to set up MSVC, then runs
 * `native-image.exe` directly in the same session.  This avoids the
 * fragile pattern of capturing the vcvars environment via `set` and
 * replaying it through Java's ProcessBuilder, which silently loses
 * PATH entries on some JVM / Windows combinations.
 */

object MsvcEnvironment {

  // Lower threshold to ensure native-image's internal paths (which can add 100-130+ chars
  // for deeply nested source files) don't exceed Windows 260-char MAX_PATH limit.
  // Native-image creates paths like: native-sources\graal\com\oracle\svm\...\Target_ClassName.c
  private val pathLengthLimit = 90

  /*
   * Call `native-image.exe` inside a vcvars-initialized cmd.exe session.
   *
   * Rather than capturing the vcvars environment and replaying it (which is
   * fragile — Java's ProcessBuilder env handling on Windows can silently lose
   * PATH entries set by vcvars64.bat), we write a small batch file that:
   *   1. calls vcvars64.bat  (sets up MSVC in the session)
   *   2. runs native-image.exe directly (inherits the live session env)
   *
   * @return process exit code.
   */
  def msvcNativeImageProcess(
    command: Seq[String],
    workingDir: os.Path,
    logger: Logger
  ): Int = {
    // Shorten the working dir for native-image (it creates deeply nested internal
    // paths that can exceed the Windows 260-char MAX_PATH limit).
    val (nativeImageWorkDir, driveToUnalias) =
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

          // show aliased drive map
          getSubstMappings.foreach((k, v) => logger.message(s"substMap  $k: -> $v"))

          val vcvarsCmd = vcvars.toIO.getAbsolutePath

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

          // Quote arguments that contain batch-special characters
          val quotedArgs = updatedCommand.map { arg =>
            if arg.exists(c => " &|^<>()".contains(c)) then s""""$arg""""
            else arg
          }.mkString(" ")

          // Build a batch file that:
          //   1. calls vcvars64.bat (with the inherited, non-SUBST CWD)
          //   2. locates cl.exe and passes it explicitly to native-image
          //      (works around GraalVM native-image not finding cl.exe via
          //       PATH when the process runs from a SUBST-drive CWD)
          //   3. switches to the shortened SUBST working directory
          //   4. runs native-image.exe
          val batchContent =
            s"""@call "$vcvarsCmd"
               |@if errorlevel 1 exit /b %ERRORLEVEL%
               |@set GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME=native-image
               |@for /f "delims=" %%i in ('where cl.exe 2^>nul') do @set "CL_EXE=%%i"
               |@if not defined CL_EXE (
               |  echo cl.exe not found in PATH after vcvars 1>&2
               |  exit /b 1
               |)
               |@cd /d "$nativeImageWorkDir"
               |@$quotedArgs --native-compiler-path="%CL_EXE%"
               |""".stripMargin
          val batchFile = os.temp(suffix = ".bat", contents = batchContent)

          logger.debug(s"native-image w/args: $updatedCommand")

          try
            // Don't pass cwd here — let cmd.exe inherit the parent's real
            // (non-SUBST) CWD so that vcvars64.bat runs without SUBST issues.
            // The batch file does `cd /d` to the shortened workdir before
            // launching native-image.
            val result = os.proc(cmdExe, "/c", batchFile.toString).call(
              stdout = os.Inherit,
              stderr = os.Inherit,
              check = false
            )
            result.exitCode
          finally
            try os.remove(batchFile)
            catch { case _: Exception => }
      }
    }
    finally
      driveToUnalias.foreach(unaliasDriveLetter)
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
