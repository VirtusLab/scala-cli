package scala.cli.tests

import java.io.File
import java.nio.charset.Charset

import scala.util.Properties

object TestUtil {

  val isNativeCli = System.getenv("IS_NATIVE_SCALA_CLI").contains("true")
  val cli = {
    val path = System.getenv("SCALA_CLI")
    if (isNativeCli)
      Seq(path)
    else
      Seq("java", "-Xmx512m", "-jar", path)
  }

  lazy val canRunJs = !isNativeCli || !Properties.isWin
  lazy val canRunNative = !Properties.isWin

  def output(root: os.Path, check: Boolean = true)(command: os.Shellable*): String = {
    val res = os.proc(command: _*).call(cwd = root, check = check)
    val rawOutput = new String(res.out.bytes, Charset.defaultCharset())
    rawOutput
      .linesIterator
      .filter(line => !line.startsWith("Compiling ") && !line.startsWith("Compiled "))
      .mkString("\n")
  }

  def run(root: os.Path, check: Boolean = true)(command: os.Shellable*): os.CommandResult =
    os.proc(command: _*).call(
      cwd = root,
      stdin = os.Inherit,
      stdout = os.Inherit,
      stderr = os.Inherit,
      check = check
    )

  def fromPath(app: String): Option[String] = {

    val pathExt =
      if (Properties.isWin)
        Option(System.getenv("PATHEXT"))
          .toSeq
          .flatMap(_.split(File.pathSeparator).toSeq)
      else
        Seq("")
    val path = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparator))
      .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, app + ext)

    candidates
      .filter(_.canExecute)
      .toStream
      .headOption
      .map(_.getAbsolutePath)
  }

}
