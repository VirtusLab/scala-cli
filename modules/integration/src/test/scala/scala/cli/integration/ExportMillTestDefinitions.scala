package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import os.RelPath

import java.nio.charset.Charset

abstract class ExportMillTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs
    with ExportCommonTestDefinitions
    with ExportScalaOrientedBuildToolsTestDefinitions
    with MillTestHelper { _: TestScalaVersion =>
  override val prepareTestInputs: TestInputs => TestInputs = _.withMillJvmOpts

  override val outputDir: RelPath = millOutputDir
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

  def jvmTestScalacOptions(className: String): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion, className).withMillJvmOpts.fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
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

  def jvmTestCompilerPlugin(mainClass: String): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion, mainClass).withMillJvmOpts.fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
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

  if (runExportTests) {
    test("JVM custom project name") {
      val customProjectName = "newproject"
      jvmTest(
        mainArgs = Seq(s"$customProjectName.run"),
        testArgs = Seq(s"$customProjectName.test"),
        extraExportArgs = Seq("-p", customProjectName),
        mainClassName = "Hello"
      )
    }
    test("JVM scalac options") {
      jvmTestScalacOptions("Hello")
    }
  }
  if (runExportTests && !actualScalaVersion.startsWith("3."))
    test("JVM with compiler plugin") {
      jvmTestCompilerPlugin("Hello")
    }

  test("Scala Native") {
    // FIXME this should be adjusted to Scala Native 0.5.x syntax once Mill gets support for it
    simpleTest(
      ExportTestProjects.nativeTest(actualScalaVersion, useNative04Syntax = true),
      mainClass = None
    )
  }
}
