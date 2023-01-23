package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class LoggingTests extends ScalaCliSuite {
  test("single -q should not suppresses compilation errors") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           |  println("Hello"
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, ".", "-q").call(cwd = root, check = false, mergeErrIntoOut = true)
      val output = res.out.trim()
      expect(output.contains("Hello.scala:3:1"))
      expect(output.contains("error"))
    }
  }
  test("single -q should not suppresses output from app") {
    TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val res    = os.proc(TestUtil.cli, ".", "-q").call(cwd = root)
      val output = res.out.trim()
      expect(output.contains("Hello"))
    }
  }
}
