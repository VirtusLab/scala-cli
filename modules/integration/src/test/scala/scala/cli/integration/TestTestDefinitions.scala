package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.annotation.tailrec

abstract class TestTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private val jvmOptions =
    // seems munit requires this with Scala 3
    if (actualScalaVersion.startsWith("3.")) Seq("--jvm", "11")
    else Nil
  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions ++ jvmOptions

  val successfulTestInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """using lib "org.scalameta::munit::0.7.25"
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
        """using lib "org.scalameta::munit::0.7.25"
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
        """using lib "com.lihaoyi::utest::0.7.10"
          |import utest._
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
        """using lib "com.lihaoyi::utest::0.7.10"
          |import utest._
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
        """using lib "com.lihaoyi::utest::0.7.10"
          |import utest._
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
        """using lib "com.novocode:junit-interface:0.11"
          |import org.junit.Test
          |
          |class MyTests {
          |
          |  @Test
          |  def foo(): Unit = {
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
        """using lib "org.scalameta::munit::0.7.25"
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests1")
          |  }
          |}
          |""".stripMargin,
      os.rel / "OtherTests.scala" ->
        """using lib "org.scalameta::munit::0.7.25"
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

  val successfulWeaverInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """using libs com.disneystreaming::weaver-cats:0.7.6, com.eed3si9n.expecty::expecty:0.15.4+5-f1d8927e-SNAPSHOT
          |import weaver._
          |import cats.effect.IO
          |
          |object MyTests extends SimpleIOSuite {
          |  test("bar") {
          |    IO.println("Hello from " + "tests").map(_ => expect(1 + 1 == 2))
          |  }
          |}
          |""".stripMargin
    )
  )

  test("successful test") {
    successfulTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  def successfulJsTest(): Unit =
    successfulTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("successful test JS") {
      successfulJsTest()
    }

  def successfulNativeTest(): Unit =
    successfulTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
    test("successful test native") {
      successfulNativeTest()
    }

  test("failing test") {
    failingTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  def failingJsTest(): Unit =
    failingTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("failing test JS") {
      failingJsTest()
    }

  def failingNativeTest(): Unit =
    failingTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
    test("failing test native") {
      failingNativeTest()
    }

  test("failing test return code") {
    failingTestInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "test", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        check = false
      )
      expect(res.exitCode == 1)
    }
  }

  test("utest") {
    successfulUtestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  def utestJs(): Unit =
    successfulUtestJsInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunJs)
    test("utest JS") {
      utestJs()
    }

  def utestNative(): Unit =
    successfulUtestNativeInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
    test("utest native") {
      utestNative()
    }

  test("junit") {
    successfulJunitInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  test("several tests") {
    severalTestsInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests1"))
      expect(output.contains("Hello from tests2"))
    }
  }

  test("weaver") {
    successfulWeaverInputs.fromRoot { root =>
      val output =
        os.proc(
          TestUtil.cli,
          "test",
          extraOptions,
          ".",
          "-r",
          "sonatype:snapshots"
        ).call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  val platforms = {
    val maybeJs = if (TestUtil.canRunJs) Seq("JS" -> Seq("--js")) else Nil
    val maybeNative =
      if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
        Seq("Native" -> Seq("--native"))
      else
        Nil
    Seq("JVM" -> Nil) ++ maybeJs ++ maybeNative
  }

  for ((platformName, platformArgs) <- platforms)
    test(s"test framework arguments $platformName") {
      val inputs = TestInputs(
        Seq(
          os.rel / "MyTests.scala" ->
            """using lib "org.scalatest::scalatest::3.2.9"
              |import org.scalatest._
              |import org.scalatest.flatspec._
              |import org.scalatest.matchers._
              |
              |class Tests extends AnyFlatSpec with should.Matchers {
              |  "A thing" should "thing" in {
              |    assert(2 + 2 == 4)
              |  }
              |}
              |""".stripMargin
        )
      )
      inputs.fromRoot { root =>
        val baseRes = os.proc(TestUtil.cli, "test", extraOptions, platformArgs, ".")
          .call(cwd = root)
        val baseOutput = baseRes.out.text()
        expect(baseOutput.contains("A thing"))
        expect(baseOutput.contains("should thing"))
        val baseShouldThingLine = baseRes.out
          .lines()
          .find(_.contains("should thing"))
          .getOrElse(???)
        expect(!baseShouldThingLine.contains("millisecond"))

        val res = os.proc(TestUtil.cli, "test", extraOptions, platformArgs, ".", "--", "-oD")
          .call(cwd = root)
        val output = res.out.text()
        expect(output.contains("A thing"))
        expect(output.contains("should thing"))
        val shouldThingLine = res.out.lines().find(_.contains("should thing")).getOrElse(???)
        expect(shouldThingLine.contains("millisecond"))
      }
    }

  for ((platformName, platformArgs) <- platforms)
    test(s"custom test framework $platformName") {
      val inputs = TestInputs(
        Seq(
          os.rel / "MyTests.scala" ->
            """using lib "com.lihaoyi::utest::0.7.10"
              |
              |package mytests
              |import utest._
              |
              |object MyTests extends TestSuite {
              |  val tests = Tests {
              |    test("foo") {
              |      assert(2 + 2 == 4)
              |      println("Hello from " + "tests")
              |    }
              |  }
              |}
              |""".stripMargin,
          os.rel / "CustomFramework.scala" ->
            """package custom
              |
              |class CustomFramework extends utest.runner.Framework {
              |  override def setup(): Unit =
              |    println("Hello from CustomFramework")
              |}
              |""".stripMargin
        )
      )
      inputs.fromRoot { root =>
        val baseRes = os.proc(TestUtil.cli, "test", extraOptions, platformArgs, ".")
          .call(cwd = root)
        val baseOutput = baseRes.out.text()
        expect(baseOutput.contains("Hello from tests"))
        expect(!baseOutput.contains("Hello from CustomFramework"))

        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "test", extraOptions, platformArgs, ".",
          "--test-framework", "custom.CustomFramework"
        )
        // format: on
        val res    = os.proc(cmd).call(cwd = root)
        val output = res.out.text()
        expect(output.contains("Hello from tests"))
        expect(output.contains("Hello from CustomFramework"))
      }
    }

  for ((platformName, platformArgs) <- platforms)
    test(s"Fail if no tests were run $platformName") {
      val inputs = TestInputs(
        Seq(
          os.rel / "MyTests.scala" ->
            """using lib "org.scalameta::munit::0.7.25"
              |
              |object MyTests
              |""".stripMargin
        )
      )

      inputs.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "test", extraOptions, "--require-tests", platformArgs, ".")
          .call(cwd = root, stderr = os.Pipe, mergeErrIntoOut = true, check = false)
        expect(res.exitCode != 0)
        val output = res.out.text()
        expect(output.contains("Error: no tests were run") || output.contains("No tests were run"))
      }
    }

  private def countSubStrings(input: String, subString: String): Int = {
    @tailrec
    def helper(startIdx: Int, acc: Int): Int =
      if (startIdx + subString.length > input.length) acc
      else {
        val idx = input.indexOf(subString, startIdx)
        if (idx < 0) acc
        else helper(idx + subString.length, acc + 1)
      }

    helper(0, 0)
  }

  test("Cross-tests") {
    val supportsNative = TestUtil.canRunNative && actualScalaVersion.startsWith("2.")
    val platforms = {
      var pf = Seq("jvm")
      if (TestUtil.canRunJs)
        pf = pf :+ "js"
      if (supportsNative)
        pf = pf :+ "native"
      pf.mkString(" ")
    }
    val inputs = {
      var inputs0 = TestInputs(
        Seq(
          os.rel / "MyTests.scala" ->
            s"""using lib "org.scalameta::munit::0.7.25"
               |using $platforms
               |
               |class MyTests extends munit.FunSuite {
               |  test("shared") {
               |    println("Hello from " + "shared")
               |  }
               |}
               |""".stripMargin,
          os.rel / "MyJvmTests.scala" ->
            """using target jvm
              |
              |class MyJvmTests extends munit.FunSuite {
              |  test("jvm") {
              |    println("Hello from " + "jvm")
              |  }
              |}
              |""".stripMargin
        )
      )
      if (TestUtil.canRunJs)
        inputs0 = inputs0.add(
          os.rel / "MyJsTests.scala" ->
            """using target js
              |
              |class MyJsTests extends munit.FunSuite {
              |  test("js") {
              |    println("Hello from " + "js")
              |  }
              |}
              |""".stripMargin
        )
      if (supportsNative)
        inputs0 = inputs0.add(
          os.rel / "MyNativeTests.scala" ->
            """using target native
              |
              |class MyNativeTests extends munit.FunSuite {
              |  test("native") {
              |    println("Hello from " + "native")
              |  }
              |}
              |""".stripMargin
        )
      inputs0
    }
    inputs.fromRoot { root =>
      val res    = os.proc(TestUtil.cli, "test", extraOptions, ".", "--cross").call(cwd = root)
      val output = res.out.text()
      val expectedCount = 1 + (if (TestUtil.canRunJs) 1 else 0) + (if (supportsNative) 1 else 0)
      expect(countSubStrings(output, "Hello from shared") == expectedCount)
      expect(output.contains("Hello from jvm"))
      if (TestUtil.canRunJs)
        expect(output.contains("Hello from js"))
      if (supportsNative)
        expect(output.contains("Hello from native"))
    }
  }

}
