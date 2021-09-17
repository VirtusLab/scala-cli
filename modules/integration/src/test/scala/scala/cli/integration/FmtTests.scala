package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class FmtTests extends munit.FunSuite {

  val simpleInputs = TestInputs(
    Seq(
      os.rel / ".scalafmt.conf" ->
        """runner.dialect = scala213
          |""".stripMargin,
      os.rel / "Foo.scala" ->
        """package foo
          |
          |    object Foo       extends       java.lang.Object  {
          |                     def           get()             = 2
          | }
          |""".stripMargin
    )
  )
  val expectedSimpleInputsFormattedContent = noCrLf {
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

}
