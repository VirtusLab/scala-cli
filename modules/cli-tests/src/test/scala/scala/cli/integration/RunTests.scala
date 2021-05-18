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
      val output = os.proc(TestUtil.cli, fileName).call(cwd = root).out.text.trim
      if (!ignoreErrors)
        assert(output == message, s"got '$output', expected '$message'")
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
      val output = os.proc(TestUtil.cli, fileName, "--js").call(cwd = root).out.text.trim
      assert(output == message)
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
      val output = os.proc(TestUtil.cli, fileName, "--native").call(cwd = root).out.text.trim
      assert(output == message)
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc", "--js").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc", "--native").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
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
      val output = os.proc(TestUtil.cli, "dir", "--main-class", "print").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
    }
  }

  test("Current directory as default") {
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
      val output = os.proc(TestUtil.cli, "run", "--main-class", "print").call(cwd = root / "dir").out.text.trim
      expect(output == message)
    }
  }

  test("No default input when no explicit command is passed") {
    val inputs = TestInputs(
      Seq(
        os.rel / "dir" / "print.sc" ->
         s"""println("Foo")
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--main-class", "print")
        .call(cwd = root / "dir", check = false, mergeErrIntoOut = true)
      val output = res.out.text.trim
      expect(res.exitCode != 0)
      expect(output.contains("No inputs provided"))
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
      val output = os.proc(TestUtil.cli, "dir", "--js", "--main-class", "print").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
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
      val output = os.proc(TestUtil.cli, "dir", "--native", "--main-class", "print").call(cwd = root).out.text.trim
      assert(output == message, s"got '$output', expected '$message'")
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

  test("sub-directory and script") {
    val fileName = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
    val inputs = TestInputs(
      Seq(
        os.rel / "dir" / "Messages.scala" ->
         s"""object Messages {
            |  def msg = "Hello"
            |}
            |""".stripMargin,
        os.rel / "dir" / "Print.scala" ->
         s"""object Print {
            |  def main(args: Array[String]): Unit =
            |    println(Messages.msg)
            |}
            |""".stripMargin,
        scriptPath ->
         s"""println(getClass.getName)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "dir", scriptPath.toString)
        .call(cwd = root)
        .out.text
        .trim
      expect(output == expectedClassName)
    }
  }
}
