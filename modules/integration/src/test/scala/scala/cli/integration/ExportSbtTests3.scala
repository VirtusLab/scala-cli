package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.charset.Charset

// format: off
class ExportSbtTests3 extends ExportSbtTestDefinitions(
  scalaVersionOpt = Some(Constants.scala3)
) {
  // format: on

  test("repository") {
    val testFile =
      s"""using scala $actualScalaVersion
         |using com.github.jupyter:jvm-repr:0.4.0
         |using repository jitpack
         |import jupyter._
         |object Test:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "sbt"
         |    println(message)
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

  test("main class") {
    val testFile =
      s"""using scala $actualScalaVersion
         |
         |object Test:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "sbt"
         |    println(message)
         |""".stripMargin
    val otherTestFile =
      s"""object Other:
         |  def main(args: Array[String]): Unit =
         |    val message = "Hello from " + "other file"
         |    println(message)
         |""".stripMargin
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala"  -> testFile,
        os.rel / "Other.scala" -> otherTestFile
      )
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "export",
        extraOptions,
        "--sbt",
        "-o",
        "sbt-proj",
        ".",
        "--main-class",
        "Test"
      )
        .call(cwd = root, stdout = os.Inherit)
      val res    = os.proc(sbt, "run").call(cwd = root / "sbt-proj")
      val output = res.out.text(Charset.defaultCharset())
      expect(output.contains("Hello from sbt"))
    }
  }

}
