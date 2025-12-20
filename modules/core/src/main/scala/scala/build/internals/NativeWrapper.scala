package scala.build.internals

//> using dep "com.lihaoyi::os-lib:0.11.6"

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.Locale

/*
- Run `vcvarsall.bat` **only to capture environment**
- Merge MSVC env with your base env
- Build a UTF‑16LE environment block
- Launch the *real* `native-image.exe` directly
- Bypass all batch files
- Preserve Ctrl-C behavior

- Directly spawn NativeImage without intervening batch files.
- MSVC environment
- Direct invocation of `native-image.exe`
 */

// ============================================================================
// Top-level launcher: capture msvc env → merge env → launch native-image.exe
// ============================================================================
object NativeImageLauncher {
  def main(args: Array[String]): Unit = {
    if !scala.util.Properties.isWin then
      sys.error(s"not valid except in Windows")

    if args.isEmpty then
      System.err.println("Usage: nativeImageLauncher.sc <native-image args>")
      sys.exit(1)

    val arch    = "x64"
    val msvcEnv = MsvcEnvironment.captureVcvarsEnv(Seq(arch))

    // Merge PATH carefully: MSVC PATH first
    val baseEnv    = sys.env
    val mergedPath =
      msvcEnv.getOrElse("PATH", "") + ";" + baseEnv.getOrElse("PATH", "")

    val mergedEnv =
      baseEnv ++ msvcEnv + ("PATH"              -> mergedPath) +
        ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

    // Locate real native-image.exe
    val graalHome      = os.Path(sys.env("GRAALVM_HOME"))
    val nativeImageExe =
      graalHome / "lib" / "svm" / "bin" / "native-image.exe"

    val exit = WindowsProcessLauncher.runInNewGroup(
      command = Seq(nativeImageExe.toString) ++ args,
      cwd = os.pwd,
      env = mergedEnv
    )

    sys.exit(exit)
  }
}

object MsvcEnvironment {

  // =========================
  // Capture MSVC environment
  // =========================
  def captureVcvarsEnv(args: Seq[String]): Map[String, String] = {
    val vcvars    = vcvarsBatchFile
    val vcvarsCmd = vcvars.replace('/', '\\')

    val cmd = Seq(
      "cmd.exe",
      "/c",
      s"""call "$vcvarsCmd" ${args.mkString(" ")} & set"""
    )

    val out = new StringBuilder
    val res = os.proc(cmd).call(
      stdout = os.ProcessOutput.Readlines(line => out.append(line).append("\n")),
      stderr = os.Inherit,
      check = false
    )

    if res.exitCode != 0 then
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
  }

  def vcvarsBatchFile: String = {
    val vswhere = findVswhere()
    val vsRoot  = findVsInstallRoot(vswhere)
    s"$vsRoot/VC/Auxiliary/Build/vcvarsall.bat"
  }

  def findVswhere(): String = {
    val candidates = Seq(
      "C:/Program Files (x86)/Microsoft Visual Studio/Installer/vswhere.exe",
      "C:/Program Files/Microsoft Visual Studio/Installer/vswhere.exe",
      "vswhere.exe"
    )
    candidates.find(p => new java.io.File(p).exists).getOrElse {
      sys.error("vswhere.exe not found")
    }
  }

  def findVsInstallRoot(vswhere: String): String = {
    val cmd    = Seq(vswhere, "-latest", "-property", "installationPath")
    val output = scala.sys.process.Process(cmd).!!.trim
    if output.isEmpty then sys.error("vswhere returned no installationPath")
    output.replace('\\', '/')
  }
}

// ============================================================================
// 2. Build UTF-16LE environment block for CreateProcessW
// ============================================================================
object EnvBlockBuilder {

  def buildEnvironmentBlock(env: Map[String, String]): Array[Byte] = {
    // Windows requires sorted order
    val entries = env.toSeq.sortBy(_._1).map { case (k, v) => s"$k=$v" }

    // Join with null terminators and add final null
    val joined = entries.mkString("\u0000") + "\u0000\u0000"

    joined.getBytes(StandardCharsets.UTF_16LE)
  }
}

object WindowsProcessLauncher {

  def runInNewGroup(
    command: Seq[String],
    cwd: os.Path,
    env: Map[String, String]
  ): Int = {

    // 1. Replace native-image.cmd with native-image.exe if applicable
    val updatedCommand =
      command.headOption match {
        case Some(cmd) if cmd.toLowerCase.endsWith("native-image.cmd") =>
          val graalHome = Paths.get(cmd).getParent.getParent
          val realExe   = graalHome.resolve("lib/svm/bin/native-image.exe")
          if (Files.exists(realExe)) realExe.toString +: command.tail
          else command
        case _ =>
          command
      }

    // 2. Augment environment (equivalent to what the .cmd wrapper does)
    val augmentedEnv =
      env + ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

    // 3. Launch using ProcessBuilder (works in JVM + native-image)
    val pb = new ProcessBuilder(updatedCommand: _*)
    pb.directory(cwd.toIO)

    val pbEnv = pb.environment()
    pbEnv.clear()
    augmentedEnv.foreach { case (k, v) => pbEnv.put(k, v) }

    // 4. Inherit IO so Ctrl-C works normally
    val proc = pb.inheritIO().start()

    // 5. Wait for completion
    proc.waitFor()
  }
}
