package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class CleanTests extends munit.FunSuite {

  test("simple") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """object Hello {
            |  def main(args: Array[String]): Unit =
            |    println("Hello")
            |}
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )

    inputs.fromRoot { root =>
      val dir = root / ".scala"

      val res = os.proc(TestUtil.cli, "run").call(cwd = root)
      expect(res.out.text.trim == "Hello")
      expect(os.exists(dir))

      val cleanRes = os.proc(TestUtil.cli, "clean").call(cwd = root)
      expect(!os.exists(dir))
    }
  }
}
