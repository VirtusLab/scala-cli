package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class CleanTests extends ScalaCliSuite {

  override def group = ScalaCliSuite.TestGroup.First

  test("simple") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """object Hello {
            |  def main(args: Array[String]): Unit =
            |    println("Hello")
            |}
            |""".stripMargin
      )
    )

    inputs.fromRoot { root =>
      val dir      = root / Constants.workspaceDirName
      val bspEntry = root / ".bsp" / "scala-cli.json"

      val res = os.proc(TestUtil.cli, "run", ".").call(cwd = root)
      expect(res.out.text().trim == "Hello")
      expect(os.exists(dir))
      expect(os.exists(bspEntry))

      os.proc(TestUtil.cli, "clean", ".").call(cwd = root)
      expect(!os.exists(dir))
      expect(!os.exists(bspEntry))
    }
  }
}
