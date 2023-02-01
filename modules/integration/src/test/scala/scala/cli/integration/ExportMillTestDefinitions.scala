package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.util.Properties

abstract class ExportMillTestDefinitions(override val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs with ExportCommonTestDefinitions {

  protected def launcher: os.RelPath =
    if (Properties.isWin) os.rel / "mill.bat"
    else os.rel / "mill"

  implicit class MillTestInputs(inputs: TestInputs) {
    def withMillJvmOpts: TestInputs = inputs.add(
      os.rel / ".mill-jvm-opts" ->
        """-Xmx512m
          |-Xms128m
          |""".stripMargin
    )
  }

  override val prepareTestInputs: TestInputs => TestInputs = _.withMillJvmOpts

  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "export",
      extraOptions,
      "--mill",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, args: String*): os.proc =
    os.proc(root / outputDir / launcher, args)

  protected val defaultProjectName      = "project"
  override val runMainArgs: Seq[String] = Seq(s"$defaultProjectName.run")

  override val runTestsArgs: Seq[String] = Seq(s"$defaultProjectName.test")

  def jvmTestScalacOptions(): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion).withMillJvmOpts.fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
      val res =
        buildToolCommand(root, "--disable-ticker", "show", s"$defaultProjectName.scalacOptions")
          .call(cwd = root / outputDir)
      val output = res.out.text(Charset.defaultCharset())
      expect(output.filterNot(_.isWhitespace) == "[\"-deprecation\"]")
    }

  def jvmTestCompilerPlugin(): Unit =
    ExportTestProjects.jvmTest(actualScalaVersion).withMillJvmOpts.fromRoot { root =>
      exportCommand(".").call(cwd = root, stdout = os.Inherit)
      locally {
        // scalacPluginIvyDeps
        val res =
          buildToolCommand(
            root,
            "--disable-ticker",
            "show",
            s"$defaultProjectName.scalacPluginIvyDeps"
          )
            .call(cwd = root / outputDir)
        val output = res.out.text(Charset.defaultCharset())
        expect(output.contains("com.olegpy"))
        expect(output.contains("better-monadic-for"))
      }
      locally {
        // test
        val res = buildToolCommand(root, s"$defaultProjectName.test").call(cwd = root / outputDir)
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
        extraExportArgs = Seq("-p", customProjectName)
      )
    }
    test("JVM scalac options") {
      jvmTestScalacOptions()
    }
  }
  if (runExportTests && !actualScalaVersion.startsWith("3."))
    test("JVM with compiler plugin") {
      jvmTestCompilerPlugin()
    }
}
