package scala.cli.tests

import com.eed3si9n.expecty.Expecty.expect

class TestTests extends munit.FunSuite {

  val successfulTestInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`org.scalameta::munit::0.7.25`
          |import $ivy.`org.scala-lang:scala-reflect:2.12.13` // seems munit lacks this dependency on the JVM
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests")
          |  }
          |}
          |""".stripMargin
    )
  )

  val failingTestInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`org.scalameta::munit::0.7.25`
          |import $ivy.`org.scala-lang:scala-reflect:2.12.13` // seems munit lacks this dependency
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 5, "Hello from " + "tests")
          |  }
          |}
          |""".stripMargin
    )
  )

  val successfulUtestInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`com.lihaoyi::utest::0.7.9`, utest._
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 4)
          |      println("Hello from " + "tests")
          |    }
          |  }
          |}
          |""".stripMargin
    )
  )

  val successfulUtestJsInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`com.lihaoyi::utest::0.7.9`, utest._
          |import scala.scalajs.js
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 4)
          |      val console = js.Dynamic.global.console
          |      console.log("Hello from " + "tests")
          |    }
          |  }
          |}
          |""".stripMargin
    )
  )

  val successfulUtestNativeInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`com.lihaoyi::utest::0.7.9`, utest._
          |import scala.scalanative.libc._
          |import scala.scalanative.unsafe._
          |
          |object MyTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 4)
          |      Zone { implicit z =>
          |        stdio.printf(toCString("Hello from " + "tests\n"))
          |      }
          |    }
          |  }
          |}
          |""".stripMargin
    )
  )

  val successfulJunitInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`com.novocode:junit-interface:0.11`
          |import org.junit.Test
          |
          |class MyTests {
          |
          |  @Test
          |  def foo() {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests")
          |  }
          |}
          |""".stripMargin
    )
  )

  val severalTestsInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """import $ivy.`org.scalameta::munit::0.7.25`
          |import $ivy.`org.scala-lang:scala-reflect:2.12.13` // seems munit lacks this dependency on the JVM
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests1")
          |  }
          |}
          |""".stripMargin,
      os.rel / "OtherTests.scala" ->
        """import $ivy.`org.scalameta::munit::0.7.25`
          |import $ivy.`org.scala-lang:scala-reflect:2.12.13` // seems munit lacks this dependency on the JVM
          |
          |class OtherTests extends munit.FunSuite {
          |  test("bar") {
          |    assert(1 + 1 == 2)
          |    println("Hello from " + "tests2")
          |  }
          |}
          |""".stripMargin
    )
  )

  test("successful test") {
    successfulTestInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".")
      expect(output.contains("Hello from tests"))
    }
  }

  def successfulJsTest(): Unit =
    successfulTestInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".", "--js")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("successful test JS") {
      successfulJsTest()
    }

  def successfulNativeTest(): Unit =
    successfulTestInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".", "--native")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative)
    test("successful test native") {
      successfulNativeTest()
    }

  test("failing test") {
    failingTestInputs.fromRoot { root =>
      val output = TestUtil.output(root, check = false)(TestUtil.cli, "test", ".")
      expect(output.contains("Hello from tests"))
    }
  }

  def failingJsTest(): Unit =
    failingTestInputs.fromRoot { root =>
      val output = TestUtil.output(root, check = false)(TestUtil.cli, "test", ".", "--js")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("failing test JS") {
      failingJsTest()
    }

  def failingNativeTest(): Unit =
    failingTestInputs.fromRoot { root =>
      val output = TestUtil.output(root, check = false)(TestUtil.cli, "test", ".", "--native")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative)
    test("failing test native") {
      failingNativeTest()
    }

  test("failing test return code") {
    failingTestInputs.fromRoot { root =>
      val res = TestUtil.run(root, check = false)(TestUtil.cli, "test", ".")
      expect(res.exitCode == 1)
    }
  }

  test("utest") {
    successfulUtestInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".")
      expect(output.contains("Hello from tests"))
    }
  }

  def utestJs(): Unit =
    successfulUtestJsInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".", "--js")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("utest JS") {
      utestJs()
    }

  def utestNative(): Unit =
    successfulUtestNativeInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".", "--native")
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative)
    test("utest native") {
      utestNative()
    }

  test("junit") {
    successfulJunitInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".")
      expect(output.contains("Hello from tests"))
    }
  }

  test("several tests") {
    severalTestsInputs.fromRoot { root =>
      val output = TestUtil.output(root)(TestUtil.cli, "test", ".")
      expect(output.contains("Hello from tests1"))
      expect(output.contains("Hello from tests2"))
    }
  }

}
