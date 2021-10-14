package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class CompileTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  val simpleInputs = TestInputs(
    Seq(
      os.rel / "MyTests.scala" ->
        """using lib com.lihaoyi::utest::0.7.10
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

  test("no arg") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions, ".").call(cwd = root).out.text()
    }
  }

  test("exit code") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" ->
          """object Main {
            |  def main(args: Array[String]): Unit =
            |    println(nope)
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
    }
  }

  test("test scope") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" ->
          """using lib "com.lihaoyi::utest:0.7.10"
            |
            |object Main {
            |  val err = utest.compileError("pprint.log(2)")
            |  def message = "Hello from " + "tests"
            |  def main(args: Array[String]): Unit = {
            |    println(message)
            |    println(err)
            |  }
            |}
            |""".stripMargin,
        os.rel / "Tests.scala" ->
          """using lib "com.lihaoyi::pprint:0.6.6"
            |using target test
            |
            |import utest._
            |
            |object Tests extends TestSuite {
            |  val tests = Tests {
            |    test("message") {
            |      assert(Main.message.startsWith("Hello"))
            |    }
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "compile", extraOptions, ".").call(cwd = root)
    }
  }

  test("test scope error") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" ->
          """object Main {
            |  def message = "Hello from " + "tests"
            |  def main(args: Array[String]): Unit =
            |    println(message)
            |}
            |""".stripMargin,
        os.rel / "Tests.scala" ->
          """using lib "com.lihaoyi::utest:0.7.10"
            |using target test
            |
            |import utest._
            |
            |object Tests extends TestSuite {
            |  val tests = Tests {
            |    test("message") {
            |      pprint.log(Main.message)
            |      assert(Main.message.startsWith("Hello"))
            |    }
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
      val expectedInOutput =
        if (actualScalaVersion.startsWith("2."))
          "not found: value pprint"
        else
          "Not found: pprint"
      expect(res.out.text().contains(expectedInOutput))
    }
  }

  test("code in test error") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" ->
          """object Main {
            |  def message = "Hello from " + "tests"
            |  def main(args: Array[String]): Unit = {
            |    zz // zz value
            |    println(message)
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "compile", extraOptions, ".")
        .call(cwd = root, check = false, stderr = os.Pipe, mergeErrIntoOut = true)
      expect(res.exitCode == 1)
      val expectedInOutput =
        if (actualScalaVersion.startsWith("2."))
          "not found: value zz"
        else
          "Not found: zz"
      val output = res.out.text()
      expect(output.contains(expectedInOutput))
      // errored line should be printed too
      expect(output.contains("zz // zz value"))
      if (actualScalaVersion.startsWith("2.12."))
        // seems the ranges returned by Bloop / scalac are only one character wide in 2.12
        expect(output.contains("^"))
      else
        // underline should have length 2
        expect(output.contains("^^"))
      expect(!output.contains("^^^"))
    }
  }

  test("compilation fails if jvm version is mismatched".only) {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" -> "object Main{java.util.Optional.of(1).isEmpty}" // isEmpty came in JDK9
      )
    )
    inputs.fromRoot{ root =>
      os.proc(TestUtil.cs,"--jvm","11", "bloop","exit").call(cwd=root, check=false)
      os.proc(TestUtil.cs,"--jvm","11", "bloop","about").call(cwd=root, check=false)
      val res = os.proc(TestUtil.cli, "compile", extraOptions, "--jvm", "8", ".")
        .call(cwd=root, check=false)
      expect(res.exitCode != 0)
    }
  }
}
