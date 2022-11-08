package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

trait RunScalaJsTestDefinitions { _: RunTestDefinitions =>
  def simpleJsTest(extraArgs: String*): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js", extraArgs).call(cwd =
        root
      ).out.trim()
      expect(output.linesIterator.toSeq.last == message)
    }
  }

  test("simple script JS") {
    simpleJsTest()
  }
  test("simple script JS in release mode") {
    simpleJsTest("--js-mode", "release")
  }

  test("simple script JS command") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(
        TestUtil.cli,
        extraOptions,
        fileName,
        "--js",
        "--command",
        "--scratch-dir",
        root / "stuff"
      )
        .call(cwd = root).out.trim()
      val command      = output.linesIterator.toVector
      val actualOutput = os.proc(command).call(cwd = root).out.trim()
      expect(actualOutput.linesIterator.toSeq.last == message)
    }
  }

  test("esmodule import JS") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""//> using jsModuleKind "es"
           |import scala.scalajs.js
           |import scala.scalajs.js.annotation._
           |
           |@js.native
           |@JSImport("console", JSImport.Namespace)
           |object console extends js.Object {
           |  def log(msg: js.Any): Unit = js.native
           |}
           |
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js")
        .call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("simple script JS via config file") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""//> using platform "scala-js"
           |import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("simple script JS via platform option") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""//> using platform "scala-native"
           |import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, ".", "--platform", "js").call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("Multiple scripts JS") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "print.sc" ->
        s"""import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |console.log(messages.msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--js").call(cwd =
        root
      ).out.trim()
      expect(output == message)
    }
  }

  def jsDomTest(): Unit = {
    val inputs = TestInputs(
      os.rel / "JsDom.scala" ->
        s"""|//> using lib "org.scala-js::scalajs-dom::2.1.0"
            |
            |import org.scalajs.dom.document
            |
            |object JsDom extends App {
            |  val pSize = document.querySelectorAll("p")
            |  println("Hello from js dom")
            |}
            |""".stripMargin
    )
    inputs.fromRoot { root =>
      // install jsdom library
      val npmPath = TestUtil.fromPath("npm").getOrElse("npm")
      os.proc(npmPath, "init", "private").call(cwd = root)
      os.proc(npmPath, "install", "jsdom").call(cwd = root)

      val output = os.proc(TestUtil.cli, extraOptions, ".", "--js", "--js-dom")
        .call(cwd = root)
        .out.text()
      expect(output.contains("Hello from js dom"))
    }
  }

  if (TestUtil.isCI)
    test("Js DOM") {
      jsDomTest()
    }

  test("help js") {
    val helpJsOption  = "--help-js"
    val helpJs        = os.proc(TestUtil.cli, "run", helpJsOption).call(check = false)
    val lines         = removeAnsiColors(helpJs.out.trim()).linesIterator.toVector
    val jsVersionHelp = lines.find(_.contains("--js-version")).getOrElse("")
    expect(jsVersionHelp.contains(s"(${Constants.scalaJsVersion} by default)"))
    expect(lines.exists(_.contains("Scala.js options")))
    expect(!lines.exists(_.contains("Scala Native options")))
  }

  test("Directory JS") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "dir" / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "dir" / "print.sc" ->
        s"""import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |console.log(messages.msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--js", "--main-class", "print_sc")
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }
}
