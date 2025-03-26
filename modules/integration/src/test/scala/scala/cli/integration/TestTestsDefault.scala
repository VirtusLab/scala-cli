package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.cli.integration.Constants.munitVersion

class TestTestsDefault extends TestTestDefinitions with TestDefault {
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
        """//> using scala 2.13
          |//> using dep com.lihaoyi::utest::0.7.10
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
      val compileRes = os.proc(TestUtil.cli, "compile", "--print-class-path", extraOptions, ".")
        .call(cwd = root)
      val cp = compileRes.out.trim().split(File.pathSeparator)
      expect(cp.length == 1) // only class dir, no scala JARs
      os.proc(TestUtil.cli, "test", extraOptions, ".")
        .call(cwd = root, stdout = os.Inherit)
    }
  }

  test(
    s"successful test --cross $actualScalaVersion with ${Constants.scala213} and ${Constants.scala212}"
  ) {
    val crossVersions   = Seq(actualScalaVersion, Constants.scala213, Constants.scala212)
    val expectedMessage = "Hello"
    TestInputs(
      os.rel / "Cross.test.scala" ->
        s"""//> using dep org.scalameta::munit::$munitVersion
           |class MyTests extends munit.FunSuite {
           |  test("foo") {
           |    assert(2 + 2 == 4)
           |    println("$expectedMessage")
           |  }
           |}
           |""".stripMargin,
      os.rel / "project.scala" -> s"//> using scala ${crossVersions.mkString(" ")}"
    ).fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--cross", "--power")
        .call(cwd = root).out.text()
      def countOccurrences(a: String, b: String): Int =
        if (b.isEmpty) 0 // Avoid infinite splitting
        else a.sliding(b.length).count(_ == b)
      expect(output.contains(expectedMessage))
      expect(countOccurrences(output, expectedMessage) == crossVersions.length)
    }
  }
}
