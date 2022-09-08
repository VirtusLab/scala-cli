package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

class TestTestsDefault extends TestTestDefinitions(scalaVersionOpt = None) {

  test("Pure Java with Scala tests") {
    val inputs = TestInputs(
      os.rel / "Messages.java" ->
        """package messages;
          |
          |public final class Messages {
          |  public final static String HELLO = "Hello";
          |}
          |""".stripMargin,
      os.rel / "test" / "MessagesTests.scala" ->
        """//> using scala "2.13"
          |//> using lib "com.lihaoyi::utest::0.7.10"
          |package messages
          |package tests
          |import utest._
          |
          |object MessagesTests extends TestSuite {
          |  val tests = Tests {
          |    test("hello") {
          |      assert(Messages.HELLO == "Hello")
          |    }
          |  }
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val compileRes = os.proc(TestUtil.cli, "compile", "--print-class-path", baseExtraOptions, ".")
        .call(cwd = root)
      val cp = compileRes.out.trim().split(File.pathSeparator)
      expect(cp.length == 1) // only class dir, no scala JARs
      os.proc(TestUtil.cli, "test", baseExtraOptions, ".")
        .call(cwd = root, stdout = os.Inherit)
    }
  }
}
