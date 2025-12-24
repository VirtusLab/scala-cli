package scala.build.internals
import java.util.Locale

/*
 * Directly invoke `native-image.exe`:
 *   run `vcvarsall.bat` **only to capture environment**
 *   merge MSVC environment and base environments.
 *   directly spawn native-image.exe
 * Avoids problematic Ctrl-C behavior of .bat / .cmd files.
 */

object MsvcEnvironment {

  // =========================
  // Capture MSVC environment
  // =========================
  def captureVcvarsEnv(vcvars: os.Path): Map[String, String] = {
    val vcvarsCmd = vcvars.toIO.getAbsolutePath

    val cmd = Seq(
      "cmd.exe",
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

  def windowsNativeImageProcess(
    command: Seq[String],
    cwd: os.Path,
    env: Map[String, String]
  ): Int = {

    def resolveNativeImage(graalHome: os.Path): Option[os.Path] = {
      val candidates = Seq(
        graalHome / "lib" / "svm" / "bin" / "native-image.exe",
        graalHome / "bin" / "native-image.exe",
        graalHome / "native-image.exe"
      )
      candidates.find(os.exists)
    }

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

    // 2. Augment environment (equivalent to what the .cmd wrapper does)
    val augmentedEnv =
      env + ("GRAALVM_ARGUMENT_VECTOR_PROGRAM_NAME" -> "native-image")

    // 3. Launch using ProcessBuilder (works in JVM + native-image)
    val pb = new ProcessBuilder(updatedCommand*)
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
