package scala.build.internals
import java.util.Locale

import scala.annotation.tailrec
import scala.build.Logger
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

  /*
   * Call `native-image.exe` with captured vcvarsall.bat environment.
   * @return process exit code.
   */
  def msvcNativeImageProcess(
    command: Seq[String],
    workingDir: os.Path,
    logger: Logger
  ): Int = {
    val vcvOpt = vcvarsOpt
    vcvOpt match {
      case None =>
        logger.debug(s"not found: vcvars64.bat")
        -1
      case Some(vcvars) =>
        logger.debug(s"Using vcvars script $vcvars")

        val (debugEcho: Seq[String], msvcEnv: Map[String, String]) =
          captureVcvarsEnv(vcvars, workingDir)
        debugEcho.foreach { dbg =>
          logger.message(s"$dbg")
        }
        val msvcEntries: Seq[String] = msvcEnv.getOrElse("PATH", "").split(";").toSeq

        // show aliased drive map
        val substMap: Map[Char, String] = aliasedDriveLetters
        substMap.foreach((k, v) => logger.message(s"substMap  $k: -> $v"))

        val finalPath = msvcEntries.mkString(";")
        val finalEnv  =
          msvcEnv +
            ("PATH"                                 -> finalPath) +
            ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

        logger.message(s"msvc PATH entries:")
        msvcEntries.foreach { entry =>
          logger.message(s"$entry;")
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

        logger.message(s"native-image w/args: $command")

        val result =
          os.proc(updatedCommand)
            .call(
              cwd = workingDir,
              env = finalEnv,
              stdout = os.Inherit,
              stderr = os.Inherit
            )

        result.exitCode
    }
  }

  def aliasedDriveLetters: Map[Char, String] =
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

  def availableDriveLetter(): Char = {
    // if a drive letter has already been mapped by SUBST, it isn't free
    val substDrives: Set[Char] = aliasedDriveLetters.keySet
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

  // =========================
  // Capture MSVC environment
  // =========================
  private def captureVcvarsEnv(
    vcvars: os.Path,
    workingDir: os.Path
  ): (Seq[String], Map[String, String]) = {

    val vcvarsCmd = vcvars.toIO.getAbsolutePath

    val sentinel = "::sentinel"
    val cmd      = Seq(
      cmdExe,
      "/c",
      s"""set "VSCMD_DEBUG=1" & call "$vcvarsCmd" & echo $sentinel & set"""
    )

    val out = new StringBuilder

    val res = os.proc(cmd).call(
      cwd = workingDir,
      env = sys.env,
      stdout = os.ProcessOutput.Readlines(line => out.append(line).append("\n")),
      stderr = os.Inherit,
      check = false
    )

    if res.exitCode != 0 then
      System.err.println(s"vcvars call failed with exit code ${res.exitCode}")
      (Seq.empty, Map.empty)
    else
      val lines = out.result().linesIterator.map(_.trim).filter(_.nonEmpty).toVector

      // Split at sentinel
      val (debugLines, afterSentinel) = lines.span(_ != sentinel)

      // Drop the sentinel itself
      val envLines = afterSentinel.drop(1)

      // Parse KEY=VALUE lines
      val envMap =
        envLines
          .flatMap { line =>
            if line.contains("=") then
              line.split("=", 2) match
                case Array(k, v) =>
                  Some(k.toUpperCase(Locale.ROOT) -> v)
                case _ =>
                  None
            else None
          }
          .toMap

      (debugLines, envMap)
  }

  private def resolveNativeImage(graalHome: os.Path): Option[os.Path] = {
    val candidates = Seq(
      graalHome / "lib" / "svm" / "bin" / "native-image.exe",
      graalHome / "bin" / "native-image.exe",
      graalHome / "native-image.exe"
    )
    candidates.find(os.exists)
  }

  private def vcvarsOpt: Option[os.Path] = {
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
        """C:\""" + programFiles + """\Microsoft Visual Studio\""" + version + "\\" + edition + """\VC\Auxiliary\Build\vcvars64.bat"""
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
