package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class FmtTests extends munit.FunSuite {

  val simpleInputsUnformattedContent: String =
    """package foo
      |
      |    object Foo       extends       java.lang.Object  {
      |                     def           get()             = 2
      | }
      |""".stripMargin
  val simpleInputs: TestInputs = TestInputs(
    Seq(
      os.rel / ".scalafmt.conf" ->
        s"""|version = "${Constants.defaultScalafmtVersion}"
            |runner.dialect = scala213
            |""".stripMargin,
      os.rel / "Foo.scala" -> simpleInputsUnformattedContent
    )
  )
  val expectedSimpleInputsFormattedContent: String = noCrLf {
    """package foo
      |
      |object Foo extends java.lang.Object {
      |  def get() = 2
      |}
      |""".stripMargin
  }

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt", ".").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("no inputs") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "fmt").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == expectedSimpleInputsFormattedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val out = os.proc(TestUtil.cli, "fmt", "--check").call(cwd = root, check = false).out.text()
      val updatedContent = noCrLf(os.read(root / "Foo.scala"))
      expect(updatedContent == noCrLf(simpleInputsUnformattedContent))
      expect(noCrLf(out) == "error: --test failed\n")
    }
  }

}
