package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import os.RelPath

import java.nio.charset.Charset

abstract class ExportMillTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs
    with ExportCommonTestDefinitions
    with ExportScalaOrientedBuildToolsTestDefinitions
    with MillTestHelper { this: TestScalaVersion & TestMillVersion =>
  override val prepareTestInputs: TestInputs => TestInputs = _.withMillJvmOpts

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

  override def commonTestDescriptionSuffix = s" (Mill $millVersion & Scala $actualScalaVersion)"

  override protected def defaultExportCommandArgs: Seq[String] = Seq("--mill-version", millVersion)

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

  if runExportTests then {
    test(s"JVM custom project name$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        val customProjectName = "newproject"
        jvmTest(
          mainArgs = Seq(s"$customProjectName.run"),
          testArgs = Seq(s"$customProjectName.test"),
          extraExportArgs = Seq("-p", customProjectName) ++ defaultExportCommandArgs,
          mainClassName = "Hello"
        )
      }
    }
    test(s"JVM scalac options$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        jvmTestScalacOptions(className = "Hello", exportArgs = defaultExportCommandArgs)
      }
    }
  }
}

sealed trait TestMillVersion: 
  def millVersion: String
trait TestMill012 extends TestMillVersion: 
  self: ExportMillTestDefinitions => override def millVersion: String = Constants.mill012Version
trait TestMill10 extends TestMillVersion: 
  self: ExportMillTestDefinitions => override def millVersion: String = Constants.mill10Version

