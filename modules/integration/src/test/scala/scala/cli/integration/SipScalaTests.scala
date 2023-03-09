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
  }
  def powerArgs(isRestricted: Boolean) = if (isRestricted) Nil else Seq("--power")

  def testDirectoriesCommand(isRestricted: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, powerArgs(isRestricted), "directories").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
      if (isRestricted) {
        expect(res.exitCode == 1)
        val output = res.out.text()
        expect(output.contains(
          """This command is restricted and requires setting the `--power` option to be used"""
        ))
      }
      else expect(res.exitCode == 0)
    }

  def testPublishDirectives(isRestricted: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | //> using publish.name "my-library"
        | class A
        |""".stripMargin

    val source = root / "A.scala"
    os.write(source, code)

    val res = os.proc(TestUtil.cli, powerArgs(isRestricted), "compile", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )

    if (isRestricted) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"directive is not supported"))
    }
    else
      expect(res.exitCode == 0)
  }

  def testMarkdownOptions(isRestricted: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | println("ala")
        |""".stripMargin

    val source = root / "A.sc"
    os.write(source, code)

    val res =
      os.proc(TestUtil.cli, powerArgs(isRestricted), "--scala", "3", "--markdown", source).call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
    if (isRestricted) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"option is not supported"))
      expect(output.contains("--markdown"))
    }
    else expect(res.exitCode == 0)
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

  def testDefaultHelpOutput(isRestricted: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    for (helpOptions <- HelpTests.variants) {
      val output =
        os.proc(TestUtil.cli, powerArgs(isRestricted), helpOptions).call(cwd = root).out.trim()
      val restrictedFeaturesMentioned = output.contains("package")
      if (isRestricted) expect(!restrictedFeaturesMentioned)
      else expect(restrictedFeaturesMentioned)
    }
  }

  def testReplHelpOutput(isRestricted: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val output =
      os.proc(TestUtil.cli, powerArgs(isRestricted), "repl", "--help-full").call(cwd =
        root
      ).out.trim()
    val restrictedFeaturesMentioned   = output.contains("--amm")
    val experimentalFeaturesMentioned = output.contains("--python")
    if (isRestricted) expect(!restrictedFeaturesMentioned && !experimentalFeaturesMentioned)
    else expect(restrictedFeaturesMentioned && experimentalFeaturesMentioned)
  }

  for (isRestricted <- Seq(false, true)) {
    test(s"test directories command when restricted mode is enabled: $isRestricted") {
      testDirectoriesCommand(isRestricted)
    }
    test(s"test publish directives when restricted mode is enabled: $isRestricted") {
      testPublishDirectives(isRestricted)
    }
    test(s"test markdown options when restricted mode is enabled: $isRestricted") {
      testMarkdownOptions(isRestricted)
    }
    test(s"test default help when restricted mode is enabled: $isRestricted") {
      testDefaultHelpOutput(isRestricted)
    }
    test(s"test repl help when restricted mode is enabled: $isRestricted") {
      testReplHelpOutput(isRestricted)
    }
  }

  test("power config turn on power features") {
    TestInputs.empty.fromRoot { root =>
      val homeEnv = Map("SCALA_CLI_CONFIG" -> (root / "config" / "config.json").toString())
      // disable power features
      os.proc(TestUtil.cli, "config", "power", "false").call(cwd = root, env = homeEnv).out.trim()
      val output = os.proc(TestUtil.cli, "package").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true,
        env = homeEnv
      ).out.text().trim
      expect(output.contains(
        """This command is restricted and requires setting the `--power` option to be used"""
      ))
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
