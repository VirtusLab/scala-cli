package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import os.RelPath

import java.nio.charset.Charset

abstract class ExportMillTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs
    with ExportCommonTestDefinitions
    with ExportScalaOrientedBuildToolsTestDefinitions
    with MillTestHelper { this: TestScalaVersion =>
  override val prepareTestInputs: TestInputs => TestInputs = _.withMillJvmOpts
  override def commonTestDescriptionSuffix = s" (Mill ${Constants.defaultMillVersion})"

  override val outputDir: RelPath                    = millOutputDir
  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "--power",
      "export",
      extraOptions,
      "--mill",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, mainClass: Option[String], args: String*): os.proc =
    millCommand(root, args*)

  override def runMainArgs(mainClass: Option[String]): Seq[String] =
    Seq(s"$millDefaultProjectName.run")

  override def runTestsArgs(mainClass: Option[String]): Seq[String] =
    Seq(s"$millDefaultProjectName.test")

  def jvmTestScalacOptions(className: String, exportArgs: Seq[String]): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion, className).withMillJvmOpts.fromRoot { root =>
      exportCommand(exportArgs :+ "."*).call(cwd = root, stdout = os.Inherit)
      val res =
        buildToolCommand(
          root,
          Some(className),
          "--disable-ticker",
          "show",
          s"$millDefaultProjectName.scalacOptions"
        )
          .call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.filterNot(_.isWhitespace) == "[\"-deprecation\"]")
    }

  def jvmTestCompilerPlugin(mainClass: String, exportArgs: Seq[String]): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion, mainClass).withMillJvmOpts.fromRoot { root =>
      exportCommand(exportArgs :+ "."*).call(cwd = root, stdout = os.Inherit)
      locally {
        // scalacPluginIvyDeps
        val res =
          buildToolCommand(
            root,
            Some(mainClass),
            "--disable-ticker",
            "show",
            s"$millDefaultProjectName.scalacPluginIvyDeps"
          )
            .call(cwd = root / outputDir)
        val output = res.out.text(Charset.defaultCharset())
        expect(output.contains("com.olegpy"))
        expect(output.contains("better-monadic-for"))
      }
      locally {
        // test
        val res =
          buildToolCommand(root, Some(mainClass), s"$millDefaultProjectName.test").call(cwd =
            root / outputDir
          )
        val output = res.out.text(Charset.defaultCharset())
        expect(output.contains("1 succeeded"))
      }
    }

  for {
    millVersion <- Constants.supportedMillVersions
    millVersionArgs = Seq("--mill-version", millVersion)
    if runExportTests
  } {
    test(s"JVM custom project name (Mill $millVersion)") {
      TestUtil.retryOnCi() {
        val customProjectName = "newproject"
        jvmTest(
          mainArgs = Seq(s"$customProjectName.run"),
          testArgs = Seq(s"$customProjectName.test"),
          extraExportArgs = Seq("-p", customProjectName) ++ millVersionArgs,
          mainClassName = "Hello"
        )
      }
    }
    test(s"JVM scalac options (Mill $millVersion)") {
      TestUtil.retryOnCi() {
        jvmTestScalacOptions(className = "Hello", exportArgs = millVersionArgs)
      }
    }
    if !actualScalaVersion.startsWith("3.") then
      test(s"JVM with compiler plugin (Mill $millVersion)") {
        TestUtil.retryOnCi() {
          jvmTestCompilerPlugin(mainClass = "Hello", exportArgs = millVersionArgs)
        }
      }

    test(s"Scala Native (Mill $millVersion)") {
      // FIXME this should be adjusted to Scala Native 0.5.x syntax once Mill gets support for it
      TestUtil.retryOnCi() {
        simpleTest(
          inputs = ExportTestProjects.nativeTest(actualScalaVersion),
          mainClass = None,
          extraExportArgs = millVersionArgs
        )
      }
    }
  }
}
