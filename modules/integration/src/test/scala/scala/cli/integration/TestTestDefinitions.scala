package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.annotation.tailrec

abstract class TestTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  protected val jvmOptions: Seq[String] =
    // seems munit requires this with Scala 3
    if (actualScalaVersion.startsWith("3.")) Seq("--jvm", "11")
    else Nil
  protected lazy val baseExtraOptions: Seq[String] = TestUtil.extraOptions ++ jvmOptions
  private lazy val extraOptions: Seq[String]       = scalaVersionArgs ++ baseExtraOptions

  def successfulTestInputs(directivesString: String = "//> using dep org.scalameta::munit::0.7.29")
    : TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      s"""$directivesString
         |
         |class MyTests extends munit.FunSuite {
         |  test("foo") {
         |    assert(2 + 2 == 4)
         |    println("Hello from " + "tests")
         |  }
         |}
         |""".stripMargin
  )

  val failingTestInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "org.scalameta::munit::0.7.29"
        |
        |class MyTests extends munit.FunSuite {
        |  test("foo") {
        |    assert(2 + 2 == 5, "Hello from " + "tests")
        |  }
        |}
        |""".stripMargin
  )

  val successfulUtestInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "com.lihaoyi::utest::0.7.10"
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

  val successfulUtestJsInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "com.lihaoyi::utest::0.7.10"
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

  val successfulUtestNativeInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "com.lihaoyi::utest::0.7.10"
        |import utest._
        |import scala.scalanative.libc._
        |import scala.scalanative.unsafe._
        |
        |object MyTests extends TestSuite {
        |  val tests = Tests {
        |    test("foo") {
        |      assert(2 + 2 == 4)
        |      Zone { implicit z =>
        |       val io = StdioHelpers(stdio)
        |       io.printf(c"%s %s\n", c"Hello from", c"tests")
        |      }
        |    }
        |  }
        |}
        |""".stripMargin
  )

  val successfulScalaCheckFromCatsNativeInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using scala "2.13.8"
        |//> using platform "native"
        |//> using dep "org.typelevel::cats-kernel-laws::2.8.0"
        |
        |import org.scalacheck._
        |import Prop.forAll
        |
        |class TestSpec extends Properties("spec") {
        |  property("startsWith") = forAll { (a: String, b: String) =>
        |    (a+b).startsWith(a)
        |  }
        |  println("Hello from " + "tests")
        |}""".stripMargin
  )

  val successfulJunitInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "com.novocode:junit-interface:0.11"
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

  val severalTestsInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "org.scalameta::munit::0.7.29"
        |
        |class MyTests extends munit.FunSuite {
        |  test("foo") {
        |    assert(2 + 2 == 4)
        |    println("Hello from " + "tests1")
        |  }
        |}
        |""".stripMargin,
    os.rel / "OtherTests.test.scala" ->
      """//> using dep "org.scalameta::munit::0.7.29"
        |
        |class OtherTests extends munit.FunSuite {
        |  test("bar") {
        |    assert(1 + 1 == 2)
        |    println("Hello from " + "tests2")
        |  }
        |}
        |""".stripMargin
  )

  val successfulWeaverInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using deps "com.disneystreaming::weaver-cats:0.8.2"
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

  val successfulESModuleTestInputs: TestInputs = TestInputs(
    os.rel / "MyTests.test.scala" ->
      """//> using dep "org.scalameta::munit::0.7.29"
        |//> using jsModuleKind "esmodule"
        |import scala.scalajs.js
        |import scala.scalajs.js.annotation._
        |
        |@js.native
        |@JSImport("console", JSImport.Namespace)
        |object console extends js.Object {
        |  def log(msg: js.Any): Unit = js.native
        |}
        |
        |class MyTests extends munit.FunSuite {
        |  test("foo") {
        |    assert(2 + 2 == 4)
        |    console.log("Hello from " + "tests")
        |  }
        |}
        |""".stripMargin
  )

  val successfulMarkdownTestInputs: TestInputs = TestInputs(
    os.rel / "Test.md" ->
      """# Example Markdown File
        |This is an example for how Scala test code can be run from a Markdown file.
        |
        |## Example Snippet
        |```scala test
        |//> using dep "org.scalameta::munit:0.7.29"
        |
        |class Test extends munit.FunSuite {
        |  test("foo") {
        |    assert(2 + 2 == 4)
        |    println("Hello from tests")
        |  }
        |}
        |```
        |
        |""".stripMargin
  )

  val failingMarkdownTestInputs: TestInputs = TestInputs(
    os.rel / "Test.md" ->
      """# Example Markdown File
        |This is an example for how Scala test code can be run from a Markdown file.
        |
        |## Example Snippet
        |```scala test
        |//> using dep "org.scalameta::munit:0.7.29"
        |
        |class Test extends munit.FunSuite {
        |  test("foo") {
        |    assert(2 + 2 == 5, "Hello from tests")
        |  }
        |}
        |```
        |
        |""".stripMargin
  )

  test("successful test") {
    successfulTestInputs().fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  if (actualScalaVersion.startsWith("2"))
    test("successful test JVM 8") {
      successfulTestInputs().fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, "test", "--jvm", "8", extraOptions, ".").call(cwd = root).out.text()
        expect(output.contains("Hello from tests"))
      }
    }

  test("successful test JS") {
    successfulTestInputs().fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  test("successful test esmodule import JS") {
    successfulESModuleTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  test("run only one test from munit") {
    val inputs: TestInputs = TestInputs(
      os.rel / "MyTests.scala" ->
        """//> using dep "org.scalameta::munit::0.7.29"
          |package test
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 5, "foo")
          |  }
          |  test("bar") {
          |    assert(2 + 3 == 5)
          |    println("Hello from bar")
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res =
        os.proc(
          TestUtil.cli,
          "test",
          ".",
          extraOptions,
          "--test-only",
          "test.MyTests",
          "--",
          "*bar*"
        ).call(cwd = root)
      val output = res.out.text()
      expect(res.exitCode == 0)
      expect(output.contains("Hello from bar"))
    }
  }
  test("run only one test from utest") {
    val inputs: TestInputs = TestInputs(
      os.rel / "FooTests.test.scala" ->
        """//> using dep "com.lihaoyi::utest::0.7.10"
          |package tests.foo
          |import utest._
          |
          |object FooTests extends TestSuite {
          |  val tests = Tests {
          |    test("foo") {
          |      assert(2 + 2 == 5)
          |    }
          |  }
          |}
          |""".stripMargin,
      os.rel / "BarTests.test.scala" ->
        """//> using dep "com.lihaoyi::utest::0.7.10"
          |package tests.bar
          |import utest._
          |
          |object BarTests extends TestSuite {
          |  val tests = Tests {
          |    test("bar") {
          |      assert(2 + 2 == 4)
          |      println("Hello from bar")
          |    }
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res =
        os.proc(
          TestUtil.cli,
          "test",
          ".",
          extraOptions,
          "--test-only",
          "tests.b*"
        ).call(cwd = root)
      val output = res.out.text()
      expect(res.exitCode == 0)
      expect(output.contains("Hello from bar"))
    }
  }
  def successfulNativeTest(): Unit =
    successfulTestInputs().fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (actualScalaVersion.startsWith("2."))
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

  test("failing test JS") {
    failingTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  def failingNativeTest(): Unit =
    failingTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  if (actualScalaVersion.startsWith("2."))
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

  test("failing test return code when compiling error") {
    val inputs = TestInputs(
      os.rel / "MyTests.test.scala" ->
        """//> using dep "org.scalameta::munit::0.7.29"
          |
          |class SomeTest extends munit.FunSuite {
          |  test("failig") {
          |   val s: String = 1
          |   assert(true == true)
          | }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root, check = false)
      expect(res.exitCode == 1)
    }
  }

  test("utest") {
    successfulUtestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  test("utest JS") {
    successfulUtestJsInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  def utestNative(): Unit =
    successfulUtestNativeInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }

  test("scalacheck from cats") {
    successfulScalaCheckFromCatsNativeInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--native")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  if (actualScalaVersion.startsWith("2."))
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
          "."
        ).call(cwd = root).out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  val platforms: Seq[(String, Seq[String])] = {
    val maybeJs = Seq("JS" -> Seq("--js"))
    val maybeNative =
      if (actualScalaVersion.startsWith("2."))
        Seq("Native" -> Seq("--native"))
      else
        Nil
    Seq("JVM" -> Nil) ++ maybeJs ++ maybeNative
  }

  for ((platformName, platformArgs) <- platforms)
    test(s"test framework arguments $platformName") {
      val inputs = TestInputs(
        os.rel / "MyTests.test.scala" ->
          """//> using dep "org.scalatest::scalatest::3.2.9"
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
      inputs.fromRoot { root =>
        val baseRes = os.proc(TestUtil.cli, "test", extraOptions, platformArgs, ".")
          .call(cwd = root, check = false)
        if (baseRes.exitCode != 0) {
          println(baseRes.out.text())
          fail("scala-cli test falied", clues(baseRes.exitCode))
        }
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
        os.rel / "MyTests.test.scala" ->
          """//> using dep "com.lihaoyi::utest::0.7.10"
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
        os.rel / "CustomFramework.test.scala" ->
          """package custom
            |
            |class CustomFramework extends utest.runner.Framework {
            |  override def setup(): Unit =
            |    println("Hello from CustomFramework")
            |}
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val baseRes = os.proc(TestUtil.cli, "test", extraOptions, platformArgs, ".")
          .call(cwd = root)
        val baseOutput = baseRes.out.text()
        expect(baseOutput.contains("Hello from tests"))
        expect(!baseOutput.contains("Hello from CustomFramework"))

        val cmd = Seq[os.Shellable](
          TestUtil.cli,
          "test",
          extraOptions,
          platformArgs,
          ".",
          "--test-framework",
          "custom.CustomFramework"
        )
        val res    = os.proc(cmd).call(cwd = root)
        val output = res.out.text()
        expect(output.contains("Hello from tests"))
        expect(output.contains("Hello from CustomFramework"))
      }
    }

  for ((platformName, platformArgs) <- platforms)
    test(s"Fail if no tests were run $platformName") {
      val inputs = TestInputs(
        os.rel / "MyTests.test.scala" ->
          """//> using dep "org.scalameta::munit::0.7.29"
            |
            |object MyTests
            |""".stripMargin
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
    val supportsNative = actualScalaVersion.startsWith("2.")
    val platforms = {
      var pf = Seq("\"jvm\"", "\"js\"")
      if (supportsNative)
        pf = pf :+ "\"native\""
      pf.mkString(", ")
    }
    val inputs = {
      var inputs0 = TestInputs(
        os.rel / "MyTests.scala" ->
          s"""//> using dep "org.scalameta::munit::0.7.29"
             |//> using platform $platforms
             |
             |class MyTests extends munit.FunSuite {
             |  test("shared") {
             |    println("Hello from " + "shared")
             |  }
             |}
             |""".stripMargin,
        os.rel / "MyJvmTests.scala" ->
          """//> using target.platform "jvm"
            |
            |class MyJvmTests extends munit.FunSuite {
            |  test("jvm") {
            |    println("Hello from " + "jvm")
            |  }
            |}
            |""".stripMargin,
        os.rel / "MyJsTests.scala" ->
          """//> using target.platform "js"
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
            """//> using target.platform "native"
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
      val res =
        os.proc(TestUtil.cli, "--power", "test", extraOptions, ".", "--cross").call(cwd = root)
      val output        = res.out.text()
      val expectedCount = 2 + (if (supportsNative) 1 else 0)
      expect(countSubStrings(output, "Hello from shared") == expectedCount)
      expect(output.contains("Hello from jvm"))
      expect(output.contains("Hello from js"))
      if (supportsNative)
        expect(output.contains("Hello from native"))
    }
  }

  def jsDomTest(): Unit = {
    val inputs = TestInputs(
      os.rel / "JsDom.test.scala" ->
        s"""//> using dep "com.lihaoyi::utest::0.7.10"
           |//> using dep "org.scala-js::scalajs-dom::2.1.0"
           |
           |import utest._
           |
           |import org.scalajs.dom.document
           |
           |object MyTests extends TestSuite {
           |  val tests = Tests {
           |    test("Hello World") {
           |      assert(document.querySelectorAll("p").size == 0)
           |      println("Hello from tests")
           |    }
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      // install jsdom library
      val npmPath = TestUtil.fromPath("npm").getOrElse("npm")
      os.proc(npmPath, "init", "private").call(cwd = root)
      os.proc(npmPath, "install", "jsdom").call(cwd = root)

      val output = os.proc(TestUtil.cli, "test", extraOptions, ".", "--js", "--js-dom")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  if (TestUtil.isCI)
    test("Js DOM") {
      jsDomTest()
    }

  test("successful test Markdown with munit") {
    successfulMarkdownTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, "Test.md")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  test("failing test Markdown with munit") {
    failingMarkdownTestInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "test", extraOptions, "Test.md")
        .call(cwd = root, check = false)
        .out.text()
      expect(output.contains("Hello from tests"))
    }
  }

  if (!actualScalaVersion.startsWith("2.12")) {
    test("toolkit") {
      successfulTestInputs(s"//> using toolkit ${Constants.toolkitVersion}").fromRoot { root =>
        val output = os.proc(TestUtil.cli, "test", extraOptions, ".").call(cwd = root).out.text()
        expect(output.contains("Hello from tests"))
      }
    }

    test("toolkit from command line") {
      successfulTestInputs("").fromRoot { root =>
        val output =
          os.proc(
            TestUtil.cli,
            "test",
            extraOptions,
            ".",
            "--toolkit",
            Constants.toolkitVersion
          ).call(cwd =
            root
          ).out.text()
        expect(output.contains("Hello from tests"))
      }
    }
  }
}
