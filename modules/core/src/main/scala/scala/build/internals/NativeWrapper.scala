package scala.build.internals
import java.util.Locale

import scala.annotation.tailrec
import scala.build.Logger

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
    logger.message(s"vcvarsOpt[$vcvOpt]")
    vcvOpt match {
      case None =>
        logger.debug(s"not found: vcvars64.bat")
        -1
      case Some(vcvars) =>
        logger.debug(s"Using vcvars script $vcvars")

        val vsRoot = vcvars / os.up / os.up / os.up / os.up
        logger.debug(s"Derived vsRoot = $vsRoot")

        val toolsRoot   = vsRoot / "VC" / "Tools" / "MSVC"
        val versionDirs = os.list(toolsRoot).filter(os.isDir)

        // Find the newest MSVC version that contains cl.exe
        val maybeMsvcToolsDir =
          versionDirs.sortBy(_.last).find { dir =>
            os.exists(dir / "bin" / "Hostx64" / "x64" / "cl.exe")
          }

        maybeMsvcToolsDir match {
          case None =>
            logger.message(s"No MSVC toolchains with cl.exe found under $toolsRoot")
            -1

          case Some(msvcToolsDir) =>
            val clDir = msvcToolsDir / "bin" / "Hostx64" / "x64"
            logger.debug(s"clDir[$clDir]")
            if (!os.exists(clDir)) {
              logger.message(s"cl.exe directory missing: $clDir")
              -1
            }
            else {
              val msvcEnv  = captureVcvarsEnv(vcvars)
              val msvcPath = msvcEnv.getOrElse("PATH", "")

              val mergedPath = dedupePaths(
                msvcPath.split(";").toSeq ++ baseEntries
              ).mkString(";")

              logger.message(s"basePath.length = ${basePath.length}")
              logger.message(s"msvcPath.length = ${msvcPath.length}")
              logger.message(s"deduped length  = ${mergedPath.length}")
              logger.message(s"deduped entries:")
              mergedPath.split(";").foreach { entry =>
                logger.message(s"$entry;")
              }

              logger.debug(s"basePath[$basePath]")

              val substMap: Map[Char, String] = aliasedDriveLetters
              substMap.foreach((k, v) => logger.message(s"substMap  $k: -> $v"))

              val clOnPath = findOnPath("cl.exe", mergedPath)
              clOnPath match {
                case None =>
                  logger.message("cl.exe not found on merged PATH — aborting before native-image")
                  -1

                case Some(found) =>
                  logger.debug(s"Verified cl.exe on PATH at: $found")

                  val mergedEnv =
                    minimalBaseEnv ++
                      msvcEnv +
                      ("PATH"                                 -> mergedPath) +
                      ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

                  logger.debug(s"Launching native-image.exe with args: $command")
                  logger.debug(s"Merged PATH = $mergedPath")

                  // 1. Replace native-image.cmd with native-image.exe, if applicable
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

                  // 3. Launch using ProcessBuilder (works in JVM + native-image)
                  val pb = new ProcessBuilder(updatedCommand*)
                  pb.directory(workingDir.toIO)

                  val pbEnv = pb.environment()
                  mergedEnv.foreach { case (k, v) => pbEnv.put(k, v) }

                  // 4. Inherit IO so Ctrl-C works normally
                  val proc = pb.inheritIO().start()

                  // 5. Wait for completion
                  proc.waitFor()
              }
            }
        }
    }
  }

  def aliasedDriveLetters: Map[Char, String] =
    val (_, output) = execWindowsCmd(cmdExe, "/c", "subst")
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
    val output   = scala.io.Source.fromInputStream(p.getInputStream, "UTF-8").mkString
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
  private def captureVcvarsEnv(vcvars: os.Path): Map[String, String] = {
    val vcvarsCmd = vcvars.toIO.getAbsolutePath

    val cmd = Seq(
      cmdExe,
      "/c",
      s"""(call \"$vcvarsCmd\") && set"""
    )

    val out = new StringBuilder
    val res = os.proc(cmd).call(
      stdout = os.ProcessOutput.Readlines(line => out.append(line).append("\n")),
      stderr = os.Inherit,
      check = false
    )

    val msvcEnv = if res.exitCode != 0 then
      System.err.println(s"vcvars call failed with exit code ${res.exitCode}")
      Map.empty
    else
      out.result().linesIterator
        .map(_.trim.replaceAll("\r$", "").replace('\\', '/'))
        .filter(_.contains("="))
        .flatMap { line =>
          line.split("=", 2) match
            case Array(k, v) => Some(k.toUpperCase(Locale.ROOT) -> v)
            case _           => None
        }
        .toMap

    msvcEnv
  }

  private def resolveNativeImage(graalHome: os.Path): Option[os.Path] = {
    val candidates = Seq(
      graalHome / "lib" / "svm" / "bin" / "native-image.exe",
      graalHome / "bin" / "native-image.exe",
      graalHome / "native-image.exe"
    )
    candidates.find(os.exists)
  }

  private def minimalBaseEnv: Map[String, String] =
    sys.env.filter { case (k, _) => windowsCoreEnvKeys.contains(k) }

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

  private def dedupePaths(entries: Seq[String]): Seq[String] =
    entries
      .flatMap(_.split(";")) // flatten any accidental PATH strings
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { p =>
        try os.Path(p).toString // normalize slashes, remove trailing separators
        catch { case _: Throwable => p }
      }
      .distinct

  private def findOnPath(exe: String, path: String): Option[os.Path] = {
    val segments = path.split(";").map(_.trim).filter(_.nonEmpty)
    segments.iterator
      .map(os.Path(_, os.pwd))
      .map(_ / exe)
      .find(os.exists)
  }

  private def normalize(p: String): String =
    p.stripSuffix("\\").stripSuffix("/").toLowerCase

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

  private lazy val windowsCoreEnvKeys = Set(
    "SystemRoot",
    "ComSpec",
    "PATHEXT",
    "TEMP",
    "TMP",
    "NUMBER_OF_PROCESSORS",
    "PROCESSOR_ARCHITECTURE"
  )

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

  lazy val windowsCorePathEntries: Seq[String] =
    Seq(
      s"$systemRoot\\System32",
      s"$systemRoot",
      s"$systemRoot\\System32\\Wbem",
      s"$systemRoot\\System32\\WindowsPowerShell\\v1.0",
      s"$systemRoot\\System32\\OpenSSH"
    )

  // Extract only entries that belong to Windows core
  lazy val extractedWindowsEntries: Seq[String] =
    sys.env.getOrElse("PATH", "")
      .split(";")
      .filter(_.nonEmpty)
      .filter(p => normalize(p).startsWith(normalize(systemRoot)))
      .toSeq

  // Merge extracted entries with missing defaults
  lazy val baseEntries: Seq[String] =
    if extractedWindowsEntries.isEmpty then
      windowsCorePathEntries
    else
      (extractedWindowsEntries ++ windowsCorePathEntries).distinct

  lazy val basePath = baseEntries.mkString(";")

}
