package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.stableStdoutOf

class CleanTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  test("simple") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val dir      = root / Constants.workspaceDirName
      val bspEntry = root / ".bsp" / "scala-cli.json"

      val res = stableStdoutOf(TestUtil.cli, "run", ".")(cwd = root)
      expect(res == "Hello")
      expect(os.exists(dir))
      expect(os.exists(bspEntry))

      os.proc(TestUtil.cli, "clean", ".").call(cwd = root)
      expect(!os.exists(dir))
      expect(!os.exists(bspEntry))
    }
  }
}
