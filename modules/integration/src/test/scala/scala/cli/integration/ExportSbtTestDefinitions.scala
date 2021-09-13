package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import java.nio.charset.Charset

abstract class ExportSbtTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  private lazy val sbtLaunchJar = {
    val res =
      os.proc(TestUtil.cs, "fetch", "--intransitive", "org.scala-sbt:sbt-launch:1.5.5").call()
    val rawPath = res.out.text.trim
    val path    = os.Path(rawPath, os.pwd)
    if (os.isFile(path)) path
    else sys.error(s"Something went wrong (invalid sbt launch JAR path '$rawPath')")
  }

  protected lazy val sbt: os.Shellable =
    Seq[os.Shellable](
      "java",
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dsbt.log.noformat=true",
      "-jar",
      sbtLaunchJar
    )

  test("JVM") {
    val testFile =
      if (actualScalaVersion.startsWith("3."))
        s"""using scala $actualScalaVersion
           |using org.scala-lang::scala3-compiler:$actualScalaVersion
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + dotty.tools.dotc.config.Properties.simpleVersionString
           |    println(message)
           |  }
           |}
           |""".stripMargin
      else
        s"""using scala $actualScalaVersion
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + scala.util.Properties.versionNumberString
           |    println(message)
           |  }
           |}
           |""".stripMargin
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" -> testFile
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--sbt", "-o", "sbt-proj", ".")
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(sbt, "run").call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from " + actualScalaVersion))
    }
  }

  test("Scala.JS") {
    val testFile =
      if (actualScalaVersion.startsWith("3."))
        s"""using scala $actualScalaVersion
           |using scala-js
           |
           |import scala.scalajs.js
           |
           |object Test:
           |  def main(args: Array[String]): Unit =
           |    val console = js.Dynamic.global.console
           |    console.log("Hello from " + "sbt")
           |""".stripMargin
      else
        s"""using scala $actualScalaVersion
           |using scala-js
           |
           |import scala.scalajs.js
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val console = js.Dynamic.global.console
           |    console.log("Hello from " + "sbt")
           |  }
           |}
           |""".stripMargin
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" -> testFile
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--sbt", "-o", "sbt-proj", ".")
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(sbt, "run").call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from sbt"))
    }
  }

  def scalaNativeTest(): Unit = {
    val nl = "\\n"
    val testFile =
      if (actualScalaVersion.startsWith("3."))
        s"""using scala $actualScalaVersion
           |using scala-native
           |
           |import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |object Test:
           |  def main(args: Array[String]): Unit =
           |    val message = "Hello from " + "sbt" + "$nl"
           |    Zone { implicit z =>
           |      stdio.printf(toCString(message))
           |    }
           |""".stripMargin
      else
        s"""using scala $actualScalaVersion
           |using scala-native
           |
           |import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val message = "Hello from " + "sbt" + "$nl"
           |    Zone { implicit z =>
           |      stdio.printf(toCString(message))
           |    }
           |  }
           |}
           |""".stripMargin
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" -> testFile
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "export", extraOptions, "--sbt", "-o", "sbt-proj", ".")
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(sbt, "run").call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from sbt"))
    }
  }
  if (TestUtil.canRunNative && !actualScalaVersion.startsWith("3."))
    test("Scala Native") {
      scalaNativeTest()
    }

}
