package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class SipScalaTests extends ScalaCliSuite {

  implicit class BinaryNameOps(binaryName: String) {
    def prepareBinary(root: os.Path): os.Path = {
      val cliPath    = os.Path(TestUtil.cliPath, os.pwd)
      val ext        = if (Properties.isWin) ".exe" else ""
      val newCliPath = root / s"$binaryName$ext"
      os.copy(cliPath, newCliPath)
      if (!Properties.isWin) os.perms.set(newCliPath, "rwxr-xr-x")
      newCliPath
    }

    def isSip: Boolean = binaryName == "scala" || binaryName.endsWith("sip")
  }

  def testDirectoriesCommand(binaryName: String): Unit =
    TestInputs.empty.fromRoot { root =>
      val binary = binaryName.prepareBinary(root)

      os.proc(binary, "compile", "--help").call(cwd = root)
      os.proc(binary, "run", "--help").call(cwd = root)

      val res = os.proc(binary, "directories").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
      if (binaryName.isSip) {
        expect(res.exitCode == 1)
        val output = res.out.text()
        expect(
          "directories is not a .* sub-command and it is not a valid path to an input file or directory".r
            .unanchored
            .matches(output)
        )
      }
      else expect(res.exitCode == 0)
    }

  def testPublishDirectives(binaryName: String): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | //> using publish.name "my-library"
        | class A
        |""".stripMargin

    val source = root / "A.scala"
    os.write(source, code)

    val binary = binaryName.prepareBinary(root)

    val res = os.proc(binary, "compile", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )

    if (binaryName.isSip) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"directive is not supported"))
    }
    else
      expect(res.exitCode == 0)
  }

  def testMarkdownOptions(binaryName: String): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | println("ala")
        |""".stripMargin

    val source = root / "A.sc"
    os.write(source, code)

    val binary = binaryName.prepareBinary(root)

    val res = os.proc(binary, "--scala", "3", "--markdown", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )
    if (binaryName.isSip) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"option is not supported"))
      expect(output.contains("--markdown"))
    }
    else expect(res.exitCode == 0)
  }

  def testVersionCommand(binaryName: String): Unit =
    TestInputs.empty.fromRoot { root =>
      val binary = binaryName.prepareBinary(root)
      for { versionOption <- VersionTests.variants } {
        val version = os.proc(binary, versionOption).call(check = false)
        assert(
          version.exitCode == 0,
          clues(version, version.out.text(), version.err.text(), version.exitCode)
        )
        val expectedLauncherVersion =
          if (binaryName.isSip) "Scala code runner version:"
          else "Scala CLI version:"
        expect(version.out.text().contains(expectedLauncherVersion))
        expect(version.out.text().contains(s"Scala version (default): ${Constants.defaultScala}"))
      }
    }

  if (TestUtil.isNativeCli)
    test(s"usage instruction should point to scala when installing by cs") { // https://github.com/VirtusLab/scala-cli/issues/1662
      TestInputs.empty.fromRoot {
        root => // cs installs binaries under .app-name.aux and scala-cli should drop .aux from progName
          val binary       = "scala".prepareBinary(root)
          val csBinaryName = root / ".scala.aux"
          os.move(binary, csBinaryName)
          val output = os.proc(csBinaryName, "test", "--usage").call(check = false).out.text().trim
          val usageMsg = TestUtil.removeAnsiColors(output)
          expect(usageMsg == "Usage: scala test [options]")
      }
    }

  def testDefaultHelpOutput(binaryName: String): Unit = TestInputs.empty.fromRoot { root =>
    val binary = binaryName.prepareBinary(root)
    for (helpOptions <- HelpTests.variants) {
      val output                      = os.proc(binary, helpOptions).call(cwd = root).out.trim()
      val restrictedFeaturesMentioned = output.contains("package")
      if (binaryName.isSip) expect(!restrictedFeaturesMentioned)
      else expect(restrictedFeaturesMentioned)
    }
  }

  def testReplHelpOutput(binaryName: String): Unit = TestInputs.empty.fromRoot { root =>
    val binary                        = binaryName.prepareBinary(root)
    val output                        = os.proc(binary, "repl", "-help").call(cwd = root).out.trim()
    val restrictedFeaturesMentioned   = output.contains("--amm")
    val experimentalFeaturesMentioned = output.contains("--python")
    if (binaryName.isSip) expect(!restrictedFeaturesMentioned && !experimentalFeaturesMentioned)
    else expect(restrictedFeaturesMentioned && experimentalFeaturesMentioned)
  }

  if (TestUtil.isNativeCli)
    for (binaryName <- Seq("scala", "scala-cli", "scala-cli-sip")) {
      test(s"test directories command when run as $binaryName") {
        testDirectoriesCommand(binaryName)
      }
      test(s"test publish directives when run as $binaryName") {
        testPublishDirectives(binaryName)
      }
      test(s"test markdown options when run as $binaryName") {
        testMarkdownOptions(binaryName)
      }
      test(s"test version command when run as $binaryName") {
        testVersionCommand(binaryName)
      }
      test(s"test default help when run as $binaryName") {
        testDefaultHelpOutput(binaryName)
      }
      test(s"test repl help when run as $binaryName") {
        testReplHelpOutput(binaryName)
      }
    }

  test("power config turn on power features") {
    TestInputs.empty.fromRoot { root =>
      val homeEnv = Map("SCALA_CLI_HOME" -> root.toString())
      // disable power features
      os.proc(TestUtil.cli, "config", "power", "false").call(cwd = root, env = homeEnv).out.trim()
      val output = os.proc(TestUtil.cli, "package").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true,
        env = homeEnv
      ).out.text().trim
      expect(
        "package is not a .* sub-command and it is not a valid path to an input file or directory".r
          .unanchored
          .matches(output)
      )
      // enable power features
      os.proc(TestUtil.cli, "config", "power", "true").call(cwd = root, env = homeEnv).out.trim()
      val powerOutput = os.proc(TestUtil.cli, "package").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true,
        env = homeEnv
      ).out.text().trim
      expect(powerOutput.contains("No inputs provided"))
    }
  }
}
