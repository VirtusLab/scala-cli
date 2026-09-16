package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Properties
import scala.util.matching.Regex

trait CoursierScalaInstallationTestHelper {
  def withScalaRunnerWrapper(
    root: os.Path,
    localBin: os.Path,
    scalaVersion: String,
    localCache: Option[os.Path] = None,
    shouldCleanUp: Boolean = true
  )(f: os.Path => Unit): Unit = {
    val csRoot = localCache.getOrElse(
      os.Path(sys.env("SCALA_CLI_TMP"), os.pwd) / s"coursier-scala-wrapper-${TestUtil.cliKind}"
    )
    val cacheDir        = localCache.getOrElse(csRoot / "cache")
    val archiveCacheDir = csRoot / "arc"
    os.proc(
      TestUtil.cs,
      "install",
      "--cache",
      cacheDir,
      "--install-dir",
      localBin,
      s"scala:$scalaVersion"
    ).call(
      cwd = root,
      env = Map("COURSIER_ARCHIVE_CACHE" -> archiveCacheDir.toString)
    )
    val (launchScalaPath: os.Path, underlyingScriptPath: os.Path) =
      if (Properties.isWin) {
        val batchWrapperScript: os.Path = localBin / "scala.bat"
        val charset                     = Charset.defaultCharset().toString
        val batchWrapperContent         = new String(os.read.bytes(batchWrapperScript), charset)
        val setCommandLine              = batchWrapperContent
          .lines()
          .iterator()
          .asScala
          .toList
          .find(_.startsWith("SET CMDLINE="))
          .getOrElse("")
        val scriptPathRegex = """SET CMDLINE="(.*\\bin\\scala\.bat)" %CMD_LINE_ARGS%""".r
        val batchScript     =
          setCommandLine match { case scriptPathRegex(extractedPath) => extractedPath }
        val batchScriptPath = os.Path(batchScript)
        val oldContent      = os.read(batchScriptPath)
        val newContent      = CoursierScalaInstallationTestHelper.patchWindowsScalaScript(
          oldContent,
          TestUtil.cliPath
        )
        expect(newContent.contains(TestUtil.cliPath))
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
        val newContent      = CoursierScalaInstallationTestHelper.patchUnixScalaScript(
          os.read(scalaScriptPath),
          TestUtil.cli
        )
        expect(newContent.contains(TestUtil.cliPath))
        os.write.over(scalaScriptPath, newContent)
        scalaBinary -> scalaScriptPath
      }
    val wrapperVersion = os.proc(launchScalaPath, "version", "--cli-version")
      .call(cwd = root).out.trim()
    val cliVersion = os.proc(TestUtil.cli, "version", "--cli-version")
      .call(cwd = root).out.trim()
    expect(wrapperVersion == cliVersion)
    f(launchScalaPath)
    if (shouldCleanUp) {
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
}

object CoursierScalaInstallationTestHelper {
  private val unixOverrideStart        = "# scala-cli-it: SCALA_CLI_CMD_BASH override"
  private val unixOverrideEnd          = "# scala-cli-it: end override"
  private val unixEvalOriginal         = """eval "${SCALA_CLI_CMD_BASH[@]}" \"""
  private val unixSourcePattern: Regex =
    """(?m)^(source|\.)\s+"\$PROG_HOME/(?:libexec|bin)/cli-common-platform".*$""".r
  private val unixEvalBlockPattern: Regex =
    """eval [\s\S]*?\\(\r?\n)(?= "--prog-name scala")""".r
  private val unixOverrideBlockPattern: Regex =
    s"$unixOverrideStart[\\s\\S]*?$unixOverrideEnd\\r?\\n?".r
  private val winOverrideStart               = "rem scala-cli-it: SCALA_CLI_CMD_WIN override"
  private val winOverrideEnd                 = "rem scala-cli-it: end override"
  private val winOverrideBlockPattern: Regex =
    s"$winOverrideStart[\\s\\S]*?$winOverrideEnd\\r?\\n?".r
  private val winCliCommonPattern: Regex =
    """(?im)^call "%_PROG_HOME%\\libexec\\cli-common-platform.bat".*$""".r

  def patchUnixScalaScript(content: String, cli: Seq[String]): String =
    val withEvalRestored = unixEvalBlockPattern.replaceAllIn(
      content,
      Regex.quoteReplacement(unixEvalOriginal) + "$1"
    )
    val withoutOldOverride = unixOverrideBlockPattern.replaceAllIn(withEvalRestored, "")
    val bashArray          = cli.map(arg => "\"" + arg.replace("\"", "\\\"") + "\"").mkString(" ")
    val overrideBlock      =
      s"""$unixOverrideStart
         |SCALA_CLI_CMD_BASH=($bashArray)
         |$unixOverrideEnd
         |""".stripMargin
    unixSourcePattern.findFirstMatchIn(withoutOldOverride) match
      case Some(m) =>
        val (before, after) = withoutOldOverride.splitAt(m.end)
        val trimmedAfter    = after.dropWhile(c => c == '\r' || c == '\n')
        val nl = if before.contains("\r\n") || after.contains("\r\n") then "\r\n" else "\n"
        s"$before$nl$overrideBlock$nl$trimmedAfter"
      case None =>
        withoutOldOverride.replace(
          unixEvalOriginal,
          s"""eval "${cli.mkString(s"\" \\${System.lineSeparator()}")}" \\"""
        )

  def patchWindowsScalaScript(content: String, cliPath: String): String =
    val withoutOldOverride = winOverrideBlockPattern.replaceAllIn(content, "")
    val overrideBlock      =
      s"""$winOverrideStart
         |set "SCALA_CLI_CMD_WIN=$cliPath"
         |$winOverrideEnd
         |""".stripMargin
    val legacyInvoke = "call %SCALA_CLI_CMD_WIN%"
    if withoutOldOverride.contains(legacyInvoke) then
      withoutOldOverride.replace(
        legacyInvoke,
        s"""set "SCALA_CLI_CMD_WIN=$cliPath"
           |call %SCALA_CLI_CMD_WIN%""".stripMargin
      )
    else
      winCliCommonPattern.findFirstMatchIn(withoutOldOverride) match
        case Some(m) =>
          val (before, after) = withoutOldOverride.splitAt(m.end)
          val trimmedAfter    = after.dropWhile(c => c == '\r' || c == '\n')
          val nl              =
            if before.contains("\r\n") || after.contains("\r\n") then "\r\n" else "\n"
          s"$before$nl$nl$overrideBlock$nl$trimmedAfter"
        case None =>
          withoutOldOverride
}
