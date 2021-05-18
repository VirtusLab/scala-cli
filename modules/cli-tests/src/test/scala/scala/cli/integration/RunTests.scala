package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class RunTests extends munit.FunSuite {

  def simpleScriptTest(ignoreErrors: Boolean = false): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
         s"""val msg = "$message"
            |println(msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, fileName)
      if (!ignoreErrors)
        assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  // warm-up run that downloads compiler bridges
  // The "Downloading compiler-bridge (from bloop?) pollute the output, and would make the first test fail.
  simpleScriptTest(ignoreErrors = true)

  test("simple script") {
    simpleScriptTest()
  }

  def simpleJsTest(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
         s"""import scala.scalajs.js
            |val console = js.Dynamic.global.console
            |val msg = "$message"
            |console.log(msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, fileName, "--js")
      assert(output0 == message)
    }
  }

  if (TestUtil.canRunJs)
    test("simple script JS") {
      simpleJsTest()
    }

  def platformNl = if (Properties.isWin) "\\r\\n" else "\\n"

  def simpleNativeTests(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
         s"""import scala.scalanative.libc._
            |import scala.scalanative.unsafe._
            |
            |Zone { implicit z =>
            |  stdio.printf(toCString("$message$platformNl"))
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, fileName, "--native")
      assert(output0 == message)
    }
  }

  if (TestUtil.canRunNative)
    test("simple script native") {
      simpleNativeTests()
    }

  test("Multiple scripts") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "print.sc" ->
         s"""println(messages.msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "print.sc", "messages.sc")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  def multipleScriptsJs(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "print.sc" ->
         s"""import scala.scalajs.js
            |val console = js.Dynamic.global.console
            |console.log(messages.msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "print.sc", "messages.sc", "--js")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  if (TestUtil.canRunJs)
    test("Multiple scripts JS") {
      multipleScriptsJs()
    }

  def multipleScriptsNative(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "print.sc" ->
         s"""import scala.scalanative.libc._
            |import scala.scalanative.unsafe._
            |
            |Zone { implicit z =>
            |  stdio.printf(toCString(messages.msg + "$platformNl"))
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "print.sc", "messages.sc", "--native")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  if (TestUtil.canRunNative)
    test("Multiple scripts native") {
      multipleScriptsNative()
    }


  test("Directory") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "dir" / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "dir" / "print.sc" ->
         s"""println(messages.msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "dir", "--main-class", "print")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  def directoryJs(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "dir" / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "dir" / "print.sc" ->
         s"""import scala.scalajs.js
            |val console = js.Dynamic.global.console
            |console.log(messages.msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "dir", "--js", "--main-class", "print")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  if (TestUtil.canRunJs)
    test("Directory JS") {
      directoryJs()
    }

  def directoryNative(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "dir" / "messages.sc" ->
         s"""def msg = "$message"
            |""".stripMargin,
        os.rel / "dir" / "print.sc" ->
         s"""import scala.scalanative.libc._
            |import scala.scalanative.unsafe._
            |
            |Zone { implicit z =>
            |  stdio.printf(toCString(messages.msg + "$platformNl"))
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output0 = TestUtil.output(root)(TestUtil.cli, "dir", "--native", "--main-class", "print")
      assert(output0 == message, s"got '$output0', expected '$message'")
    }
  }

  if (TestUtil.canRunNative)
    test("Directory native") {
      directoryNative()
    }

  test("sub-directory") {
    val fileName = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
    val inputs = TestInputs(
      Seq(
        scriptPath ->
         s"""println(getClass.getName)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, scriptPath.toString)
        .call(cwd = root)
        .out.text
        .trim
      expect(output == expectedClassName)
    }
  }

}
