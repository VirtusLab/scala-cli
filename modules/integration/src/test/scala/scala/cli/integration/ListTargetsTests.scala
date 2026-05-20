package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ListTargetsTests extends ScalaCliSuite {

  private def normalize(s: String): String = s.replaceAll("\\s", "")

  test("list-targets emits the full platform x scala-version matrix") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using platforms jvm native
          |//> using scala 3.6.4 3.5.0
          |@main def hello = println("hi")
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "list-targets", ".").call(cwd = root)
      val out = normalize(res.out.text())

      val expected = normalize(
        """[
          |  { "platform": "JVM",    "scalaVersion": "3.6.4" },
          |  { "platform": "Native", "scalaVersion": "3.6.4" },
          |  { "platform": "JVM",    "scalaVersion": "3.5.0" },
          |  { "platform": "Native", "scalaVersion": "3.5.0" }
          |]""".stripMargin
      )

      expect(out == expected)
    }
  }

  test("list-targets with no cross directives returns a single entry") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using scala 3.6.4
          |@main def hello = println("hi")
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "list-targets", ".").call(cwd = root)
      val out = normalize(res.out.text())

      val expected = normalize(
        """[
          |  { "platform": "JVM", "scalaVersion": "3.6.4" }
          |]""".stripMargin
      )

      expect(out == expected)
    }
  }
}
