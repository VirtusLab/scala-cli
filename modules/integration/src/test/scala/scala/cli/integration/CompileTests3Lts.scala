package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class CompileTests3Lts extends CompileTestDefinitions with CompileTests3StableDefinitions
    with Test3Lts {
  test("compile --print-class-path reflects --sloth toggle in the same workspace") {
    TestInputs(
      os.rel / "Main.scala" ->
        s"""object Main {
           |  lazy val greeting: String = "Hello"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val withSloth = os.proc(
        TestUtil.cli,
        "--power",
        "compile",
        extraOptions,
        slothOptions,
        "--print-class-path",
        "."
      ).call(cwd = root, stderr = os.Pipe)
      expect(withSloth.out.trim().contains(slothCacheSegment))

      val withoutSloth = os.proc(
        TestUtil.cli,
        "--power",
        "compile",
        extraOptions,
        "--print-class-path",
        "."
      ).call(cwd = root, stderr = os.Pipe)
      expect(!withoutSloth.out.trim().contains(slothCacheSegment))

      val withSlothAgain = os.proc(
        TestUtil.cli,
        "--power",
        "compile",
        extraOptions,
        slothOptions,
        "--print-class-path",
        "."
      ).call(cwd = root, stderr = os.Pipe)
      expect(withSlothAgain.out.trim().contains(slothCacheSegment))
    }
  }
}
