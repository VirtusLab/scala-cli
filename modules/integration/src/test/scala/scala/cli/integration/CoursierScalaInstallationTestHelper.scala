package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Properties

trait CoursierScalaInstallationTestHelper {
  def withScalaRunnerWrapper(
    root: os.Path,
    localCache: os.Path,
    localBin: os.Path,
    scalaVersion: String
  )(f: os.Path => Unit): Unit = {
    os.proc(
      TestUtil.cs,
      "install",
      "--cache",
      localCache,
      "--install-dir",
      localBin,
      s"scala:$scalaVersion"
    ).call(cwd = root)
    val (launchScalaPath: os.Path, underlyingScriptPath: os.Path) =
      if (Properties.isWin) {
        val batchWrapperScript: os.Path = localBin / "scala.bat"
        val charset                     = Charset.defaultCharset().toString
        val batchWrapperContent         = new String(os.read.bytes(batchWrapperScript), charset)
        val setCommandLine = batchWrapperContent
          .lines()
          .iterator()
          .asScala
          .toList
          .find(_.startsWith("SET CMDLINE="))
          .getOrElse("")
        val scriptPathRegex = """SET CMDLINE="(.*\\bin\\scala\.bat)" %CMD_LINE_ARGS%""".r
        val batchScript =
          setCommandLine match { case scriptPathRegex(extractedPath) => extractedPath }
        val batchScriptPath = os.Path(batchScript)
        val oldContent      = os.read(batchScriptPath)
        val newContent = oldContent.replace(
          "call %SCALA_CLI_CMD_WIN%",
          s"""set "SCALA_CLI_CMD_WIN=${TestUtil.cliPath}"
             |call %SCALA_CLI_CMD_WIN%""".stripMargin
        )
        expect(newContent != oldContent)
        os.write.over(batchScriptPath, newContent)
        batchWrapperScript -> batchScriptPath
      }
      else {
        val scalaBinary: os.Path = localBin / "scala"
        val fileBytes            = os.read.bytes(scalaBinary)
        val shebang              = new String(fileBytes.takeWhile(_ != '\n'), "UTF-8")
        val binaryData           = fileBytes.drop(shebang.length + 1)
        val execLine             = new String(binaryData.takeWhile(_ != '\n'), "UTF-8")
        val scriptPathRegex      = """exec "([^"]+/bin/scala).*"""".r
        val scalaScript = execLine match { case scriptPathRegex(extractedPath) => extractedPath }
        val scalaScriptPath = os.Path(scalaScript)
        val lineToChange    = "eval \"${SCALA_CLI_CMD_BASH[@]}\" \\"
        // FIXME: the way the scala script calls the launcher currently ignores the --debug flag
        val newContent = os.read(scalaScriptPath).replace(
          lineToChange,
          s"""SCALA_CLI_CMD_BASH=(\"\\\"${TestUtil.cliPath}\\\"\")
             |$lineToChange""".stripMargin
        )
        os.write.over(scalaScriptPath, newContent)
        scalaBinary -> scalaScriptPath
      }
    val wrapperVersion = os.proc(launchScalaPath, "version", "--cli-version")
      .call(cwd = root).out.trim()
    val cliVersion = os.proc(TestUtil.cli, "version", "--cli-version")
      .call(cwd = root).out.trim()
    expect(wrapperVersion == cliVersion)
    f(launchScalaPath)
    // clean up cs local binaries
    val csPrebuiltBinaryDir =
      os.Path(underlyingScriptPath.toString().substring(
        0,
        underlyingScriptPath.toString().indexOf(scalaVersion) + scalaVersion.length
      ))
    System.err.println(s"Cleaning up, trying to remove $csPrebuiltBinaryDir")
    try {
      os.remove.all(csPrebuiltBinaryDir)

      System.err.println(s"Cleanup complete. Removed $csPrebuiltBinaryDir")
    }
    catch {
      case ex: java.nio.file.FileSystemException =>
        System.err.println(s"Failed to remove $csPrebuiltBinaryDir: $ex")
    }
  }
}
