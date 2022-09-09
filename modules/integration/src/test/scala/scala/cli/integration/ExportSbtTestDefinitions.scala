package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

import scala.util.Properties

abstract class ExportSbtTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  protected def runExportTests: Boolean =
    Properties.isLinux

  private lazy val sbtLaunchJar = {
    val res =
      os.proc(TestUtil.cs, "fetch", "--intransitive", "org.scala-sbt:sbt-launch:1.5.5").call()
    val rawPath = res.out.trim()
    val path    = os.Path(rawPath, os.pwd)
    if (os.isFile(path)) path
    else sys.error(s"Something went wrong (invalid sbt launch JAR path '$rawPath')")
  }

  protected lazy val sbt: os.Shellable =
    Seq[os.Shellable](
      "java",
      "-Xmx512m",
      "-Xms128m",
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dsbt.log.noformat=true",
      "-jar",
      sbtLaunchJar
    )

  protected def simpleTest(
    inputs: TestInputs,
    extraExportArgs: Seq[String] = Nil,
    sbtArgs: Seq[String] = Seq("run")
  ): Unit =
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--sbt", "-o", "sbt-proj", ".", extraExportArgs)
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(sbt, sbtArgs).call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from exported Scala CLI project"))
    }

  def jvmTest(): Unit = {
    val inputs = ExportTestProjects.jvmTest(actualScalaVersion)
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--sbt", "-o", "sbt-proj", ".")
        .call(cwd = root, stdout = os.Inherit)
      // main
      val res    = os.proc(sbt, "run").call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from " + actualScalaVersion))
      // resource
      expect(output.contains("resource:1,2"))
      // test
      val testRes    = os.proc(sbt, "test").call(cwd = root / "sbt-proj")
      val testOutput = testRes.out.text(Charset.defaultCharset())
      expect(testOutput.contains("1 succeeded"))
    }
  }
  if (runExportTests)
    test("JVM") {
      jvmTest()
    }

  if (runExportTests)
    test("Scala.js") {
      simpleTest(ExportTestProjects.jsTest(actualScalaVersion))
    }

  if (runExportTests && !actualScalaVersion.startsWith("3."))
    test("Scala Native") {
      simpleTest(ExportTestProjects.nativeTest(actualScalaVersion))
    }

}
