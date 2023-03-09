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
  def powerArgs(isPowerMode: Boolean): Seq[String] = if (isPowerMode) Seq("--power") else Nil

  def testDirectoriesCommand(isPowerMode: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, powerArgs(isPowerMode), "directories").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
      if (!isPowerMode) {
        expect(res.exitCode == 1)
        val output = res.out.text()
        expect(output.contains(
          """This command is restricted and requires setting the '--power' launcher option to be used"""
        ))
      }
      else expect(res.exitCode == 0)
    }

  def testPublishDirectives(isPowerMode: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | //> using publish.name "my-library"
        | class A
        |""".stripMargin

    val source = root / "A.scala"
    os.write(source, code)

    val res = os.proc(TestUtil.cli, powerArgs(isPowerMode), "compile", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )

    if (!isPowerMode) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"directive is experimental"))
    }
    else
      expect(res.exitCode == 0)
  }

  def testMarkdownOptions(isPowerMode: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | println("ala")
        |""".stripMargin

    val source = root / "A.sc"
    os.write(source, code)

    val res =
      os.proc(TestUtil.cli, powerArgs(isPowerMode), "--scala", "3", "--markdown", source).call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
    if (!isPowerMode) {
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"option is experimental"))
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

  def testDefaultHelpOutput(isPowerMode: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    for (helpOptions <- HelpTests.variants) {
      val output =
        os.proc(TestUtil.cli, powerArgs(isPowerMode), helpOptions).call(cwd = root).out.trim()
      val restrictedFeaturesMentioned = output.contains("package")
      if (!isPowerMode) expect(!restrictedFeaturesMentioned)
      else expect(restrictedFeaturesMentioned)
    }
  }

  def testReplHelpOutput(isPowerMode: Boolean): Unit = TestInputs.empty.fromRoot { root =>
    val output =
      os.proc(TestUtil.cli, powerArgs(isPowerMode), "repl", "--help-full").call(cwd =
        root
      ).out.trim()
    val restrictedFeaturesMentioned   = output.contains("--amm")
    val experimentalFeaturesMentioned = output.contains("--python")
    if (!isPowerMode) expect(!restrictedFeaturesMentioned && !experimentalFeaturesMentioned)
    else expect(restrictedFeaturesMentioned && experimentalFeaturesMentioned)
  }

  for {
    isPowerMode <- Seq(false, true)
    powerModeString = if (isPowerMode) "enabled" else "disabled"
  } {
    test(s"test directories command when power mode is $powerModeString") {
      testDirectoriesCommand(isPowerMode)
    }
    test(s"test publish directives when power mode is $powerModeString") {
      testPublishDirectives(isPowerMode)
    }
    test(s"test markdown options when power mode is $powerModeString") {
      testMarkdownOptions(isPowerMode)
    }
    test(s"test default help when power mode is $powerModeString") {
      testDefaultHelpOutput(isPowerMode)
    }
    test(s"test repl help when power mode is $powerModeString") {
      testReplHelpOutput(isPowerMode)
    }
  }

  for ((restrictionType, subCommand) <- Seq("restricted" -> "package", "experimental" -> "export"))
    test(s"power config enables $restrictionType sub-command: $subCommand") {
      TestInputs.empty.fromRoot { root =>
        val homeEnv = Map("SCALA_CLI_CONFIG" -> (root / "config" / "config.json").toString())
        // disable power features
        os.proc(TestUtil.cli, "config", "power", "false").call(cwd = root, env = homeEnv).out.trim()
        val output = os.proc(TestUtil.cli, subCommand).call(
          cwd = root,
          check = false,
          mergeErrIntoOut = true,
          env = homeEnv
        ).out.text().trim
        expect(output.contains(
          s"""This command is $restrictionType and requires setting the '--power' launcher option to be used"""
        ))
        // enable power features
        os.proc(TestUtil.cli, "config", "power", "true").call(cwd = root, env = homeEnv).out.trim()
        val powerOutput = os.proc(TestUtil.cli, subCommand).call(
          cwd = root,
          check = false,
          mergeErrIntoOut = true,
          env = homeEnv
        ).out.text().trim
        expect(powerOutput.contains("No inputs provided"))
      }
    }
}
