package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import java.nio.charset.Charset
import scala.util.Properties

abstract class ExportMillTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  protected def launcher: os.RelPath =
    if (Properties.isWin) os.rel / "mill.bat"
    else os.rel / "mill"

  private def addMillJvmOpts(inputs: TestInputs): TestInputs =
    inputs.add(
      os.rel / ".mill-jvm-opts" ->
        """-Xmx512m
          |-Xms128m
          |""".stripMargin
    )

  protected def simpleTest(
    inputs: TestInputs,
    extraExportArgs: Seq[String] = Nil,
    millArgs: Seq[String] = Seq("project.run")
  ): Unit =
    addMillJvmOpts(inputs).fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "export",
        extraOptions,
        "--mill",
        "-o",
        "mill-proj",
        ".",
        extraExportArgs
      )
        .call(cwd = root, stdout = os.Inherit)
      val res = os.proc(root / "mill-proj" / launcher, "-i", millArgs)
        .call(cwd = root / "mill-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from exported Scala CLI project"))
    }

  def jvmTest(): Unit = {
    val inputs = addMillJvmOpts(ExportTestProjects.jvmTest(actualScalaVersion))
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--mill", "-o", "mill-proj", ".")
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(root / "mill-proj" / launcher, "__.run").call(cwd = root / "mill-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from " + actualScalaVersion))
    }
  }
  if (!Properties.isWin)
    test("JVM") {
      jvmTest()
    }

  if (!Properties.isWin)
    test("Scala.JS") {
      simpleTest(ExportTestProjects.jsTest(actualScalaVersion))
    }

  if (TestUtil.canRunNative && !actualScalaVersion.startsWith("3."))
    test("Scala Native") {
      simpleTest(ExportTestProjects.nativeTest(actualScalaVersion))
    }

}
