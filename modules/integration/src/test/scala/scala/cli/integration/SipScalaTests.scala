package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import os.CommandResult

import scala.util.Properties

class SipScalaTests extends ScalaCliSuite
    with SbtTestHelper
    with MillTestHelper
    with CoursierScalaInstallationTestHelper {
  implicit class StringEnrichment(s: String) {
    def containsExperimentalWarningOf(featureNameAndType: String): Boolean =
      s.contains(s"The $featureNameAndType is experimental") ||
      s.linesIterator
        .dropWhile(!_.endsWith("are marked as experimental:"))
        .takeWhile(_ != "Please bear in mind that non-ideal user experience should be expected.")
        .contains(s" - $featureNameAndType")
  }
  override def munitFlakyOK: Boolean = TestUtil.isCI

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
        os.proc(TestUtil.cli, "config", configKey, "true", "-f")
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
        expect(output.contains("The `directories` sub-command is restricted."))
      }
      else expect(res.exitCode == 0)
    }

  def testExportCommand(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit = {
    val inputs = TestInputs(
      os.rel / "HelloWorld.scala" ->
        """//> using scala 3.0.0
          |object HelloWorld extends App { println(\"Hello World\") }
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        powerArgs(isPowerMode),
        "export",
        suppressExperimentalWarningArgs(areWarningsSuppressed),
        "."
      ).call(
        cwd = root,
        check = false,
        stderr = os.Pipe
      )
      val errOutput = res.err.trim()
      isPowerMode -> areWarningsSuppressed match {
        case (false, _) =>
          expect(errOutput.contains("The `export` sub-command is experimental."))
          expect(res.exitCode == 1)
        case (true, false) =>
          expect(errOutput.containsExperimentalWarningOf("`export` sub-command"))
        case (true, true) =>
          expect(!errOutput.containsExperimentalWarningOf("`export` sub-command"))
      }
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
            "The `repositories.default` configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 1)
          expect(configPublishUserErrOutput.containsExperimentalWarningOf(
            "`publish.user.name` configuration key"
          ))
        case (true, false) =>
          expect(configProxyResult.exitCode == 0)
          expect(!configProxyErrOutput.contains(
            "The `repositories.default` configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 0)
          expect(configPublishUserErrOutput.containsExperimentalWarningOf(
            "`publish.user.name` configuration key"
          ))
        case (true, true) =>
          expect(configProxyResult.exitCode == 0)
          expect(!configProxyErrOutput.contains(
            "The `repositories.default` configuration key is restricted."
          ))
          expect(configPublishUserResult.exitCode == 0)
          expect(!configPublishUserErrOutput.contains(
            "`publish.user.name` configuration key"
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
      if (!isPowerMode) expect(output.contains("The `export` sub-command is experimental."))
      else expect(output.contains("The `export` sub-command is experimental."))
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

  def testExperimentalDirectives(isPowerMode: Boolean, areWarningsSuppressed: Boolean): Unit =
    TestInputs.empty.fromRoot { root =>
      val code =
        """//> using publish.name my-library
          |//> using python
          |class A
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
          expect(errOutput.containsExperimentalWarningOf(
            "`//> using publish.name my-library`"
          ))
          expect(errOutput.containsExperimentalWarningOf("`//> using python`"))
        case (true, true) =>
          expect(res.exitCode == 0)
          expect(!errOutput.containsExperimentalWarningOf(
            "`//> using publish.name my-library`"
          ))
          expect(!errOutput.containsExperimentalWarningOf("`//> using python`"))
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
          expect(
            errOutput.contains("Unrecognized argument: The `--markdown` option is experimental.")
          )
        case (true, false) =>
          expect(res.exitCode == 0)
          expect(errOutput.containsExperimentalWarningOf("`--markdown` option"))
        case (true, true) =>
          expect(res.exitCode == 0)
          expect(!errOutput.containsExperimentalWarningOf("`--markdown` option"))
      }
    }

  if (TestUtil.isNativeCli)
    test(
      s"usage instruction should point to scala when installing by cs"
    ) { // https://github.com/VirtusLab/scala-cli/issues/1662
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
          expect(errOutput.containsExperimentalWarningOf(featureType))
        },
      testWhenEnabled = (root, homeEnv) => {
        val errOutput = callExperimentalFeature(root, homeEnv).err.trim()
        expect(!errOutput.containsExperimentalWarningOf(featureType))
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
        s"test experimental directives when power mode is $powerModeString and experimental warnings are $warningsSuppressedString"
      ) {
        testExperimentalDirectives(isPowerMode, warningsSuppressed)
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
    testConfigSuppressingExperimentalFeatureWarnings("`export` sub-command") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        val res = os.proc(TestUtil.cli, "--power", "export")
          .call(cwd = root, check = false, env = homeEnv, stderr = os.Pipe)
        expect(res.exitCode == 1)
        res
    }
  }
  test("test global config suppressing warnings for an experimental option") {
    testConfigSuppressingExperimentalFeatureWarnings("`--md` option") {
      (root: os.Path, homeEnv: Map[String, String]) =>
        os.proc(TestUtil.cli, "--power", "-e", "println()", "--md")
          .call(cwd = root, env = homeEnv, stderr = os.Pipe)
    }
  }
  test("test global config suppressing warnings for an experimental directive") {
    testConfigSuppressingExperimentalFeatureWarnings(
      "`//> using publish.name my-library` directive"
    ) {
      (root: os.Path, homeEnv: Map[String, String]) =>
        val quote = TestUtil.argQuotationMark
        os.proc(TestUtil.cli, "--power", "-e", s"//> using publish.name ${quote}my-library$quote")
          .call(cwd = root, env = homeEnv, stderr = os.Pipe)
    }
  }
  test("test global config suppressing warnings for an experimental configuration key") {
    testConfigSuppressingExperimentalFeatureWarnings("`publish.user.name` configuration key") {
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
          expect(errOutput.contains(s"The `$subCommand` sub-command is $restrictionType."))
        },
        testWhenEnabled = { (root, homeEnv) =>
          val res = os.proc(TestUtil.cli, subCommand)
            .call(cwd = root, check = false, env = homeEnv, stderr = os.Pipe)
          expect(res.exitCode == 1)
          expect(res.err.text().trim().contains("No inputs provided"))
        }
      )
    }

  test("test multiple sources of experimental features") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using target.scope main
          |//> using target.platform jvm
          |//> using publish.name my-library
          |
          |object Main {
          |  def main(args: Array[String]): Unit = {
          |    println("Hello World!")
          |  }
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "export", ".", "--object-wrapper", "--md")
        .call(cwd = root, mergeErrIntoOut = true)

      val output = res.out.trim()

      assertNoDiff(
        output,
        s"""Some utilized features are marked as experimental:
           | - `export` sub-command
           | - `--object-wrapper` option
           | - `--md` option
           |Please bear in mind that non-ideal user experience should be expected.
           |If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
           |Exporting to a sbt project...
           |Some utilized directives are marked as experimental:
           | - `//> using publish.name my-library`
           | - `//> using target.platform jvm`
           | - `//> using target.scope main`
           |Please bear in mind that non-ideal user experience should be expected.
           |If you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team at https://github.com/VirtusLab/scala-cli
           |Exported to: ${root / "dest"}
           |""".stripMargin
      )
    }
  }

  test(s"code using scala-continuations should compile for Scala 2.12.2".flaky) {
    val sourceFileName = "example.scala"
    TestInputs(os.rel / sourceFileName ->
      """import scala.util.continuations._
        |
        |object ContinuationsExample extends App {
        |  def generator(init: Int): Int @cps[Unit] = {
        |    shift { k: (Int => Unit) =>
        |      for (i <- init to 10) k(i)
        |    }
        |    0 // We never reach this point, but it enables the function to compile.
        |  }
        |
        |  reset {
        |    val result = generator(1)
        |    println(result)
        |  }
        |}
        |""".stripMargin).fromRoot { root =>
      val continuationsVersion = "1.0.3"
      val res = os.proc(
        TestUtil.cli,
        "compile",
        sourceFileName,
        "--compiler-plugin",
        s"org.scala-lang.plugins:::scala-continuations-plugin:$continuationsVersion",
        "--dependency",
        s"org.scala-lang.plugins::scala-continuations-library:$continuationsVersion",
        "-P:continuations:enable",
        "-S",
        "2.12.2"
      )
        .call(cwd = root)
      expect(res.exitCode == 0)
    }
  }

  for {
    sv <- Seq(Constants.scala212, Constants.scala213, Constants.scala3NextRc)
    code =
      if (sv.startsWith("3")) "println(dotty.tools.dotc.config.Properties.simpleVersionString)"
      else "println(scala.util.Properties.versionNumberString)"
    anotherVersion =
      if (sv.startsWith("2.13")) Constants.scala212
      else if (sv.startsWith("2.12")) Constants.scala213
      else Constants.scala3Lts
  } {
    test(
      s"default Scala version overridden with $sv by a launcher parameter is respected when running a script"
    ) {
      TestInputs(os.rel / "simple.sc" -> code)
        .fromRoot { root =>
          val r = os.proc(
            TestUtil.cli,
            "--cli-default-scala-version",
            sv,
            "run",
            "simple.sc",
            "--with-compiler"
          )
            .call(cwd = root)
          expect(r.out.trim() == sv)
        }
    }
    test(
      s"default Scala version overridden with $sv by a launcher parameter is overridable by -S passing $anotherVersion"
    ) {
      TestInputs(os.rel / "simple.sc" -> code)
        .fromRoot { root =>
          val r = os.proc(
            TestUtil.cli,
            "--cli-default-scala-version",
            sv,
            "run",
            "simple.sc",
            "--with-compiler",
            "-S",
            anotherVersion
          )
            .call(cwd = root)
          expect(r.out.trim() == anotherVersion)
        }
    }

    test(
      s"default Scala version overridden with $sv by a launcher parameter is respected when printing Scala version"
    ) {
      TestInputs.empty.fromRoot { root =>
        val r =
          os.proc(TestUtil.cli, "--cli-default-scala-version", sv, "version", "--scala-version")
            .call(cwd = root)
        expect(r.out.trim() == sv)
      }
    }

    test(
      s"default Scala version overridden with $sv by a launcher parameter is respected when printing versions"
    ) {
      TestInputs.empty.fromRoot { root =>
        val r = os.proc(TestUtil.cli, "--cli-default-scala-version", sv, "version")
          .call(cwd = root)
        expect(r.out.trim().contains(sv))
      }
    }
  }

  test(s"default Scala version override launcher option can only be passed once") {
    TestInputs.empty.fromRoot { root =>
      val (sv1, sv2)  = (Constants.scala212, Constants.scala213)
      val launcherOpt = "--cli-default-scala-version"
      val r = os.proc(TestUtil.cli, launcherOpt, sv1, launcherOpt, sv2, "version")
        .call(cwd = root, check = false, stderr = os.Pipe)
      expect(r.exitCode == 1)
      expect(r.err.trim().contains(launcherOpt))
      expect(r.err.trim().contains("already specified"))
    }
  }

  for {
    withBloop <- Seq(true, false)
    withBloopString = if (withBloop) "with Bloop" else "with --server=false"
    sv3             = if (Properties.isWin) "3.5.0-RC1" else "3.5.0-RC1-fakeversion-bin-SNAPSHOT"
    sv2             = "2.13.15-bin-ccdcde3"
  } {
    test(
      s"default Scala version ($sv3) coming straight from a predefined local repository $withBloopString"
    ) {
      TestInputs(
        os.rel / "simple.sc" -> "println(dotty.tools.dotc.config.Properties.versionNumberString)"
      )
        .fromRoot { root =>
          val localRepoPath = root / "local-repo"
          val sv            = sv3
          if (Properties.isWin) {
            // 3.5.0-RC1-fakeversion-bin-SNAPSHOT has too long filenames for Windows.
            // Yes, seriously. Which is why we can't use it there.
            val artifactNames =
              Seq("scala3-compiler_3", "scala3-staging_3", "scala3-tasty-inspector_3") ++
                (if (withBloop) Seq("scala3-sbt-bridge") else Nil)
            for { artifactName <- artifactNames } {
              val csRes = os.proc(
                TestUtil.cs,
                "fetch",
                "--cache",
                localRepoPath,
                s"org.scala-lang:$artifactName:$sv"
              )
                .call(cwd = root)
              expect(csRes.exitCode == 0)
            }
          }
          else {
            TestUtil.initializeGit(root)
            os.proc(
              "git",
              "clone",
              "https://github.com/dotty-staging/maven-test-repo.git",
              localRepoPath.toString
            ).call(cwd = root)
          }
          val buildServerOptions =
            if (withBloop) Nil else Seq("--server=false")

          val predefinedRepository =
            if (Properties.isWin)
              (localRepoPath / "https" / "repo1.maven.org" / "maven2").toNIO.toUri.toASCIIString
            else
              (localRepoPath / "thecache" / "https" / "repo1.maven.org" / "maven2").toNIO.toUri.toASCIIString
          val r = os.proc(
            TestUtil.cli,
            "--cli-default-scala-version",
            sv,
            "--predefined-repository",
            predefinedRepository,
            "run",
            "simple.sc",
            "--with-compiler",
            "--offline",
            "--power",
            buildServerOptions
          )
            .call(cwd = root)
          expect(r.out.trim() == sv)
        }
    }

    test(
      s"default Scala version ($sv2) coming straight from a predefined local repository $withBloopString".flaky
    ) {
      TestInputs(
        os.rel / "simple.sc" -> "println(scala.util.Properties.versionNumberString)"
      )
        .fromRoot { root =>
          val localRepoPath = root / "local-repo"
          val sv            = sv2
          val artifactNames =
            Seq("scala-compiler") ++ (if (withBloop) Seq("scala2-sbt-bridge") else Nil)
          for { artifactName <- artifactNames } {
            val csRes = os.proc(
              TestUtil.cs,
              "fetch",
              "--cache",
              localRepoPath,
              "-r",
              "https://scala-ci.typesafe.com/artifactory/scala-integration",
              s"org.scala-lang:$artifactName:$sv"
            )
              .call(cwd = root)
            expect(csRes.exitCode == 0)
          }
          val buildServerOptions = if (withBloop) Nil else Seq("--server=false")
          os.proc(TestUtil.cli, "bloop", "exit", "--power").call(cwd = root)
          val r = os.proc(
            TestUtil.cli,
            "--cli-default-scala-version",
            sv,
            "--predefined-repository",
            (localRepoPath / "https" / "repo1.maven.org" / "maven2").toNIO.toUri.toASCIIString,
            "--predefined-repository",
            (localRepoPath / "https" / "scala-ci.typesafe.com" / "artifactory" / "scala-integration")
              .toNIO.toUri.toASCIIString,
            "run",
            "simple.sc",
            "--with-compiler",
            "--offline",
            "--power",
            buildServerOptions
          )
            .call(cwd = root)
          expect(r.out.trim() == sv)
        }
    }
  }

  test(s"default Scala version override launcher option is respected by the SBT export") {
    val input     = "printVersion.sc"
    val code      = """println(s"Default version: ${scala.util.Properties.versionNumberString}")"""
    val outputDir = "sbt-project"
    TestInputs(os.rel / input -> code).fromRoot { root =>
      val defaultSv       = Constants.scala213
      val expectedMessage = s"Default version: $defaultSv"
      val launcherOpt     = "--cli-default-scala-version"
      val exportRes = os.proc(
        TestUtil.cli,
        launcherOpt,
        defaultSv,
        "export",
        input,
        "--sbt",
        "--power",
        "-o",
        outputDir
      ).call(cwd = root)
      expect(exportRes.exitCode == 0)
      val sbtRes = sbtCommand("run").call(cwd = root / outputDir)
      val output = sbtRes.out.trim()
      expect(output.contains(expectedMessage))
    }
  }

  test("prog name override launcher arg allows to change the identified launcher name") {
    TestInputs.empty.fromRoot { root =>
      val progName              = "some-weird-launcher-name-some-dev-thought-of"
      val invalidSubCommandName = "foo"
      val res = os.proc(TestUtil.cli, "--prog-name", progName, invalidSubCommandName)
        .call(cwd = root, stderr = os.Pipe, check = false)
      expect(res.exitCode == 1)
      expect(res.err.trim().contains(progName))
      expect(res.err.trim().contains(invalidSubCommandName))
    }
  }

  test(s"default Scala version override launcher option is respected by the Mill export") {
    val input     = "printVersion.sc"
    val code      = """println(s"Default version: ${scala.util.Properties.versionNumberString}")"""
    val outputDir = millOutputDir
    TestInputs(os.rel / input -> code).fromRoot { root =>
      val defaultSv       = Constants.scala213
      val expectedMessage = s"Default version: $defaultSv"
      val launcherOpt     = "--cli-default-scala-version"
      val exportRes = os.proc(
        TestUtil.cli,
        launcherOpt,
        defaultSv,
        "export",
        input,
        "--mill",
        "--power",
        "-o",
        outputDir
      ).call(cwd = root)
      expect(exportRes.exitCode == 0)
      val millRes = millCommand(root, s"$millDefaultProjectName.run").call(cwd = root / outputDir)
      val output  = millRes.out.trim()
      expect(output.contains(expectedMessage))
    }
  }

  test("--with-compiler option includes scala3-staging & scala3-tasty-inspector artifacts") {
    TestInputs(os.rel / "example.sc" ->
      """import scala.quoted.staging.Compiler
        |import scala.tasty.inspector.TastyInspector
        |""".stripMargin).fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "compile",
        "example.sc",
        "--with-compiler"
      ).call(cwd = root)
      expect(res.exitCode == 0)
    }
  }

  test(s"default Scala version override launcher option is respected by the json export") {
    val input = "printVersion.sc"
    val code  = """println(s"Default version: ${scala.util.Properties.versionNumberString}")"""
    TestInputs(os.rel / input -> code).fromRoot { root =>
      val defaultSv   = Constants.scala213
      val launcherOpt = "--cli-default-scala-version"
      val exportRes = os.proc(
        TestUtil.cli,
        launcherOpt,
        defaultSv,
        "export",
        input,
        "--json",
        "--power"
      ).call(cwd = root)
      expect(exportRes.exitCode == 0)
      expect(exportRes.out.trim().contains(s""""scalaVersion": "$defaultSv""""))
    }
  }

  test("no warnings about TASTY when using the latest nightly with scripts") {
    val scriptName = "script.sc"
    TestInputs(os.rel / "script.sc" -> "println(1)").fromRoot { root =>
      val scala3Nightly =
        os.proc(
          TestUtil.cli,
          "-e",
          "println(dotty.tools.dotc.config.Properties.versionNumberString)",
          "-S",
          "3.nightly",
          "--with-compiler"
        )
          .call(cwd = root)
          .out.trim()
      val res =
        os.proc(TestUtil.cli, "--cli-default-scala-version", scala3Nightly, "run", scriptName)
          .call(cwd = root, stderr = os.Pipe)
      expect(res.exitCode == 0)
      expect(!res.err.trim().contains("TASTY"))
    }
  }

  test("--cli-version and --cli-default-scala-version can be passed in tandem") {
    TestUtil.retryOnCi() {
      TestInputs.empty.fromRoot { root =>
        val cliVersion   = "1.3.1"
        val scalaVersion = "3.5.1-RC1-bin-20240522-e0c030c-NIGHTLY"
        val res = os.proc(
          TestUtil.cli,
          "--cli-version",
          cliVersion,
          "--cli-default-scala-version",
          scalaVersion,
          "version"
        ).call(cwd = root)
        expect(res.out.trim().contains(cliVersion))
        expect(res.out.trim().contains(scalaVersion))
      }
    }
  }

  test("coursier scala installation works in --offline mode") {
    TestInputs.empty.fromRoot { root =>
      val localCache   = root / "local-cache"
      val localBin     = root / "local-bin"
      val scalaVersion = Constants.scala3NextRcAnnounced
      withScalaRunnerWrapper(
        root = root,
        localBin = localBin,
        scalaVersion = scalaVersion,
        localCache = Some(localCache)
      ) { launchScalaPath =>
        val r =
          os.proc(
            launchScalaPath,
            "--offline",
            "--power",
            "--with-compiler",
            "-e",
            "println(dotty.tools.dotc.config.Properties.versionNumberString)"
          ).call(
            cwd = root,
            env = Map("COURSIER_CACHE" -> localCache.toString),
            check = false // need to clean up even on failure
          )
        expect(r.exitCode == 0)
        expect(r.out.trim() == scalaVersion)
      }
    }
  }

  // this check is just to ensure this isn't being run for LTS RC jobs
  // should be adjusted when a new LTS line is released
  if (!Constants.scala3NextRc.startsWith(Constants.scala3LtsPrefix))
    test("scalac help respects --cli-default-scala-version") {
      TestInputs.empty.fromRoot { root =>
        val sv = Constants.scala3NextRc
        val launcherVersionOverrideHelp =
          os.proc(TestUtil.cli, "--cli-default-scala-version", sv, "--scalac-help")
            .call(cwd = root).out.trim()
        val standardVersionOverrideHelp =
          os.proc(TestUtil.cli, "--scalac-help", "-S", sv)
            .call(cwd = root).out.trim()
        val migrationPrefix = sv.take(2) + (sv.charAt(2).asDigit + 1).toString
        expect(launcherVersionOverrideHelp.contains(s"$migrationPrefix-migration"))
        expect(launcherVersionOverrideHelp == standardVersionOverrideHelp)
      }
    }

  test("coursier scala installation works with utf8 paths") {
    val utf8DirPath = os.rel / "äöü"
    TestInputs(utf8DirPath / "version.sc" ->
      "println(dotty.tools.dotc.config.Properties.versionNumberString)")
      .fromRoot { root =>
        val rootWithUtf8 = root / utf8DirPath
        val localCache   = rootWithUtf8 / "local-cache"
        val localBin     = rootWithUtf8 / "local-bin"
        val scalaVersion = Constants.scala3NextRcAnnounced
        withScalaRunnerWrapper(
          root = rootWithUtf8,
          localCache = Some(localCache),
          localBin = localBin,
          scalaVersion = scalaVersion
        ) { launchScalaPath =>
          val r = os.proc(launchScalaPath, "--with-compiler", "version.sc")
            .call(
              cwd = rootWithUtf8,
              env = Map("COURSIER_CACHE" -> localCache.toString),
              check = false // need to clean up even on failure
            )
          expect(r.exitCode == 0)
          expect(r.out.trim() == scalaVersion)
        }
      }
  }

  test("raw coursier works with utf8 paths") {
    val utf8DirPath = os.rel / "äöü"
    TestInputs(utf8DirPath / "version.sc" ->
      "println(dotty.tools.dotc.config.Properties.versionNumberString)")
      .fromRoot { root =>
        val rootWithUtf8 = root / utf8DirPath
        val localCache   = rootWithUtf8 / "local-cache"
        val localBin     = rootWithUtf8 / "local-bin"
        val scalaVersion = Constants.scala3NextRcAnnounced
        // ensure cs works at all
        os.proc(TestUtil.cs, "version")
          .call(cwd = rootWithUtf8, stdout = os.Inherit)
        // ensure scala is installable
        os.proc(
          TestUtil.cs,
          "install",
          "--cache",
          localCache,
          "--install-dir",
          localBin,
          s"scala:$scalaVersion"
        ).call(cwd = rootWithUtf8)
        // ensure scala got installed
        val launcherPath = if (Properties.isWin) localBin / "scala.bat" else localBin / "scala"
        os.proc(launcherPath, "--version")
          .call(cwd = rootWithUtf8, stdout = os.Inherit)
      }
  }
}
