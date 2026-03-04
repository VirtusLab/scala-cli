package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

import scala.cli.integration.Constants.munitVersion
import scala.cli.integration.TestUtil.StringOps

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
      expect(output.contains(expectedMessage))
      expect(output.countOccurrences(expectedMessage) == crossVersions.length)
    }
  }

  for {
    scalaVersion <- TestUtil.legacyScalaVersionsOnePerMinor
    expectedMessage = "Hello, world!"
    expectedWarning =
      s"Defaulting to a legacy test-runner module version: ${Constants.runnerScala30LegacyVersion}"
  }
    test(s"run a simple test with Scala $scalaVersion (legacy)") {
      TestInputs(os.rel / "example.test.scala" ->
        // using JUnit to work around TASTy and macro incompatibilities
        s"""//> using dep com.novocode:junit-interface:0.11
           |import org.junit.Test
           |
           |class MyTests {
           |  @Test
           |  def foo(): Unit = {
           |    assert(2 + 2 == 4)
           |    println("$expectedMessage")
           |  }
           |}
           |""".stripMargin).fromRoot { root =>
        val res =
          os.proc(TestUtil.cli, "test", ".", "-S", scalaVersion, TestUtil.extraOptions)
            .call(cwd = root, stderr = os.Pipe)
        val out = res.out.trim()
        expect(out.contains(expectedMessage))
        val err = res.err.trim()
        expect(err.contains(expectedWarning))
        expect(err.countOccurrences(expectedWarning) == 1)
      }
    }
}
