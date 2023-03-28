package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import os.CommandResult

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
  def suppressExperimentalWarningArgs(areWarningsSuppressed: Boolean): Seq[String] =
    if (areWarningsSuppressed) Seq("--suppress-experimental-feature-warning") else Nil

  def testWithGlobalConfig(
    configKey: String,
    testWhenDisabled: (os.Path, Map[String, String]) => Any,
    testWhenEnabled: (os.Path, Map[String, String]) => Any
  ): Any =
    TestInputs.empty.fromRoot { root =>
      val homeEnv = Map("SCALA_CLI_CONFIG" -> (root / "config" / "config.json").toString())
      for (disableSetting <- Seq("false", "--unset")) {
        os.proc(TestUtil.cli, "config", configKey, disableSetting)
          .call(cwd = root, env = homeEnv)
        testWhenDisabled(root, homeEnv)
        os.proc(TestUtil.cli, "config", configKey, "true")
          .call(cwd = root, env = homeEnv)
        testWhenEnabled(root, homeEnv)
      }
    }

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
        expect(output.contains("The 'directories' sub-command is restricted."))
      }
      else expect(res.exitCode == 0)
    }

  def testExportCommand(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        powerArgs(isPowerMode),
        "export",
        suppressExperimentalWarningArgs(areWarningsSuppressed)
      ).call(
        cwd = root,
        check = false,
        stderr = os.Pipe
      )
      val errOutput = res.err.trim()
      expect(res.exitCode == 1)
      isPowerMode -> areWarningsSuppressed match {
        case (false, _) =>
          expect(errOutput.contains("The 'export' sub-command is experimental."))
        case (true, false) =>
          expect(errOutput.contains("The 'export' sub-command is experimental."))
        case (true, true) =>
          expect(!errOutput.contains("The 'export' sub-command is experimental."))
      }
    }

  def testConfigCommand(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val homeEnv = Map("SCALA_CLI_CONFIG" -> (root / "config" / "config.json").toString())
      def callConfig(key: String, value: String): CommandResult =
        os.proc(
          TestUtil.cli,
          powerArgs(isPowerMode),
          "config",
          suppressExperimentalWarningArgs(areWarningsSuppressed),
          key,
          value
        ).call(cwd = root, check = false, stderr = os.Pipe, env = homeEnv)

      val configProxyResult    = callConfig("repositories.default", "https://example.address/maven")
      val configProxyErrOutput = configProxyResult.err.trim()
      val configPublishUserResult    = callConfig("publish.user.name", "exampleUser")
      val configPublishUserErrOutput = configPublishUserResult.err.trim()
      isPowerMode -> areWarningsSuppressed match {
        case (false, _) =>
          expect(configProxyResult.exitCode == 1)
          expect(configProxyErrOutput.contains(
            "The 'repositories.default' configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 1)
          expect(configPublishUserErrOutput.contains(
            "The 'publish.user.name' configuration key is experimental."
          ))
        case (true, false) =>
          expect(configProxyResult.exitCode == 0)
          expect(!configProxyErrOutput.contains(
            "The 'repositories.default' configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 0)
          expect(configPublishUserErrOutput.contains(
            "The 'publish.user.name' configuration key is experimental."
          ))
        case (true, true) =>
          expect(configProxyResult.exitCode == 0)
          expect(!configProxyErrOutput.contains(
            "The 'repositories.default' configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 0)
          expect(!configPublishUserErrOutput.contains(
            "The 'publish.user.name' configuration key is experimental."
          ))
      }
    }

  def testExportCommandHelp(isPowerMode: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, powerArgs(isPowerMode), "export", "-h").call(
        cwd = root,
        stderr = os.Pipe
      )
      val output = res.out.trim()
      if (!isPowerMode) expect(output.contains("The 'export' sub-command is experimental."))
      else expect(output.contains("The 'export' sub-command is experimental."))
    }

  def testConfigCommandHelp(isPowerMode: Boolean, isFullHelp: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val helpParams = if (isFullHelp) Seq("--full-help") else Seq("-h")
      val helpOutput = os.proc(TestUtil.cli, powerArgs(isPowerMode), "config", helpParams)
        .call(cwd = root, stderr = os.Pipe)
        .out.trim()
      if (isPowerMode) {
        expect(helpOutput.contains("(power)"))
        expect(helpOutput.contains("repositories.mirrors"))
      }
      if (isFullHelp) {
        expect(helpOutput.contains("(hidden)"))
        expect(helpOutput.contains("interactive-was-suggested"))
      }
      if (isPowerMode && isFullHelp) {
        expect(helpOutput.contains("(experimental)"))
        expect(helpOutput.contains("publish.user.name"))
      }
    }

  def testPublishDirectives(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val code =
        """
          | //> using publish.name "my-library"
          | class A
          |""".stripMargin

      val source = root / "A.scala"
      os.write(source, code)

      val res = os.proc(
        TestUtil.cli,
        powerArgs(isPowerMode),
        "compile",
        suppressExperimentalWarningArgs(areWarningsSuppressed),
        source
      ).call(
        cwd = root,
        check = false,
        stderr = os.Pipe
      )

      val errOutput = res.err.trim()
      isPowerMode -> areWarningsSuppressed match {
        case (false, _) =>
          expect(res.exitCode == 1)
          expect(errOutput.contains(s"directive is experimental"))
        case (true, false) =>
          expect(res.exitCode == 0)
          expect(errOutput.contains(
            """The '//> using publish.name "my-library"' directive is experimental."""
          ))
        case (true, true) =>
          expect(res.exitCode == 0)
          expect(!errOutput.contains(
            """The '//> using publish.name "my-library"' directive is experimental."""
          ))
      }
    }

  def testMarkdownOptions(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val code =
        """
          | println("ala")
          |""".stripMargin

      val source = root / "A.sc"
      os.write(source, code)

      val res =
        os.proc(
          TestUtil.cli,
          powerArgs(isPowerMode),
          suppressExperimentalWarningArgs(areWarningsSuppressed),
          "--scala",
          "3",
          "--markdown",
          source
        ).call(
          cwd = root,
          check = false,
          stderr = os.Pipe
        )
      val errOutput = res.err.trim()
      isPowerMode -> areWarningsSuppressed match {
        case (false, _) =>
          expect(res.exitCode == 1)
          expect(errOutput.contains(s"option is experimental"))
          expect(errOutput.contains("--markdown"))
        case (true, false) =>
          expect(res.exitCode == 0)
          expect(errOutput.contains("The '--markdown' option is experimental."))
        case (true, true) =>
          expect(res.exitCode == 0)
          expect(!errOutput.contains("The '--markdown' option is experimental."))
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

  def testConfigSuppressingExperimentalFeatureWarnings(featureType: String)(
    callExperimentalFeature: (
      os.Path,
      Map[String, String]
    ) => CommandResult
  ): Unit = {
    testWithGlobalConfig(
      "suppress-warning.experimental-features",
      testWhenDisabled =
        (root, homeEnv) => {
          val errOutput = callExperimentalFeature(root, homeEnv).err.trim()
          expect(errOutput.contains(s"$featureType is experimental."))
        },
      testWhenEnabled = (root, homeEnv) => {
        val errOutput = callExperimentalFeature(root, homeEnv).err.trim()
        expect(!errOutput.contains(s"$featureType is experimental."))
      }
    )
  }

  for {
    isPowerMode <- Seq(false, true)
    powerModeString = if (isPowerMode) "enabled" else "disabled"
  } {
    test(s"test directories command when power mode is $powerModeString") {
      testDirectoriesCommand(isPowerMode)
    }
    test(s"test default help when power mode is $powerModeString") {
      testDefaultHelpOutput(isPowerMode)
    }
    test(s"test repl help when power mode is $powerModeString") {
      testReplHelpOutput(isPowerMode)
    }
    for {
      warningsSuppressed <- Seq(true, false)
      warningsSuppressedString = if (warningsSuppressed) "suppressed" else "not suppressed"
    } {
      test(
        s"test publish directives when power mode is $powerModeString and experimental warnings are $warningsSuppressedString"
      ) {
        testPublishDirectives(isPowerMode, warningsSuppressed)
      }
      test(
        s"test markdown options when power mode is $powerModeString and experimental warnings are $warningsSuppressedString"
      ) {
        testMarkdownOptions(isPowerMode, warningsSuppressed)
      }
      test(
        s"test export command when power mode is $powerModeString and experimental warnings are $warningsSuppressedString"
      ) {
        testExportCommand(isPowerMode, warningsSuppressed)
      }
      test(
        s"test config command when power mode is $powerModeString and experimental warnings are $warningsSuppressedString"
      ) {
        testConfigCommand(isPowerMode, warningsSuppressed)
      }
    }
    test(s"test export command help output when power mode is $powerModeString") {
      testExportCommandHelp(isPowerMode)
    }
    for {
      isFullHelp <- Seq(true, false)
      helpTypeString = if (isFullHelp) "full help" else "short help"
    }
      test(s"test config command $helpTypeString output when power mode is $powerModeString") {
        testConfigCommandHelp(isPowerMode, isFullHelp)
      }
  }

  test("test global config suppressing warnings for an experimental sub-command") {
    testConfigSuppressingExperimentalFeatureWarnings("sub-command") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        val res = os.proc(TestUtil.cli, "--power", "export")
          .call(cwd = root, check = false, env = homeEnv, stderr = os.Pipe)
        expect(res.exitCode == 1)
        res
    }
  }
  test("test global config suppressing warnings for an experimental option") {
    testConfigSuppressingExperimentalFeatureWarnings("option") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        os.proc(TestUtil.cli, "--power", "-e", "println()", "--md")
          .call(cwd = root, env = homeEnv, stderr = os.Pipe)
    }
  }
  test("test global config suppressing warnings for an experimental directive") {
    testConfigSuppressingExperimentalFeatureWarnings("directive") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        val quote = TestUtil.argQuotationMark
        os.proc(TestUtil.cli, "--power", "-e", s"//> using publish.name ${quote}my-library$quote")
          .call(cwd = root, env = homeEnv, stderr = os.Pipe)
    }
  }
  test("test global config suppressing warnings for an experimental configuration key") {
    testConfigSuppressingExperimentalFeatureWarnings("configuration key") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        os.proc(TestUtil.cli, "--power", "config", "publish.user.name")
          .call(cwd = root, env = homeEnv, stderr = os.Pipe)
    }
  }

  for ((restrictionType, subCommand) <- Seq("restricted" -> "package", "experimental" -> "export"))
    test(s"power config enables $restrictionType sub-command: $subCommand") {
      testWithGlobalConfig(
        configKey = "power",
        testWhenDisabled = { (root, homeEnv) =>
          val res = os.proc(TestUtil.cli, subCommand)
            .call(cwd = root, check = false, env = homeEnv, stderr = os.Pipe)
          val errOutput = res.err.trim()
          expect(res.exitCode == 1)
          expect(errOutput.contains(s"The '$subCommand' sub-command is $restrictionType."))
        },
        testWhenEnabled = { (root, homeEnv) =>
          val res = os.proc(TestUtil.cli, subCommand)
            .call(cwd = root, check = false, env = homeEnv, stderr = os.Pipe)
          expect(res.exitCode == 1)
          expect(res.err.text().trim().contains("No inputs provided"))
        }
      )
    }
}
