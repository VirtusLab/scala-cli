package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

trait RunScalaJsTestDefinitions { _: RunTestDefinitions =>
  def simpleJsTestOutput(extraArgs: String*): String = {
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
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js", extraArgs).call(
        cwd = root,
        mergeErrIntoOut = true
      ).out.trim()
      expect(output.linesIterator.toSeq.last == message)
      output
    }
  }

  test("simple script JS") {
    simpleJsTestOutput()
  }

  test(s"simple script JS in fullLinkJs mode") {
    val output = simpleJsTestOutput("--js-mode", "fullLinkJs", "-v", "-v", "-v")
    expect(output.contains("--fullOpt"))
    expect(!output.contains("--fastOpt"))
    expect(!output.contains("--noOpt"))
  }

  test(s"simple script JS in fastLinkJs mode") {
    val output = simpleJsTestOutput("--js-mode", "fastLinkJs", "-v", "-v", "-v")
    expect(output.contains("--fastOpt"))
    expect(!output.contains("--fullOpt"))
    expect(!output.contains("--noOpt"))
  }

  test("without node on the PATH") {
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
      val thrown = os.proc(TestUtil.cli, extraOptions, fileName, "--js", "--server=false")
        .call(
          cwd = root,
          env = Map("PATH" -> "", "PATHEXT" -> ""),
          check = false,
          mergeErrIntoOut = true
        )
      val output = thrown.out.trim()

      assert(thrown.exitCode == 1)
      assert(output.contains("Node was not found on the PATH"))
    }
  }

  test("JS arguments") {
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""// FIXME Ideally, 'args' should contain 'argv' here out-of-the-box.
           |import scala.scalajs.js
           |import scala.scalajs.js.Dynamic.global
           |val process = global.require("process")
           |val argv = Option(process.argv)
           |  .filterNot(js.isUndefined)
           |  .map(_.asInstanceOf[js.Array[String]].drop(2).toSeq)
           |  .getOrElse(Nil)
           |val console = global.console
           |console.log(argv.mkString(" "))
           |""".stripMargin
    )
    val messageArgs = Seq("Hello", "foo")
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".", "--js", "--", messageArgs)
        .call(cwd = root)
        .out.trim()
      expect(output == messageArgs.mkString(" "))
    }
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
        s"""|//> using dep "org.scala-js::scalajs-dom::2.1.0"
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
      val mainClassName = if (actualScalaVersion.startsWith("3")) "print_sc" else "print"

      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--js", "--main-class", mainClassName)
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }
  test("set es version to scala-js-cli") {
    val inputs = TestInputs(
      os.rel / "run.sc" ->
        s"""//> using jsEsVersionStr "es2018"
           |
           |import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |console.log(\"\"\"(?m).\"\"\".r.findFirstIn("Hi").get)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "run.sc", "--js").call(cwd = root).out.trim()
      expect(output == "H")
    }
  }

  test("js defaults & toolkit latest") {
    val msg = "Hello"
    TestInputs(
      os.rel / "script.sc" ->
        s"""//> using toolkit latest
           |//> using platform "scala-js"
           |import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val jsonString = "{\\"msg\\": \\"$msg\\"}"
           |val json: ujson.Value  = ujson.read(jsonString)
           |console.log(json("msg").str)
           |""".stripMargin
    ).fromRoot { root =>
      val result = os.proc(TestUtil.cli, "run", "script.sc").call(cwd = root)
      expect(result.out.trim() == msg)
    }
  }
}
