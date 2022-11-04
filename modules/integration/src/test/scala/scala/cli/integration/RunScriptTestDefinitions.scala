package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

trait RunScriptTestDefinitions { _: RunTestDefinitions =>
  def simpleScriptTest(ignoreErrors: Boolean = false, extraArgs: Seq[String] = Nil): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, extraArgs, fileName).call(cwd = root).out.trim()
      if (!ignoreErrors)
        expect(output == message)
    }
  }

  test("simple script") {
    simpleScriptTest()
  }

  test("verbosity") {
    simpleScriptTest(extraArgs = Seq("-v"))
  }

  test("Multiple scripts") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "print.sc" ->
        s"""println(messages.msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc").call(cwd =
        root
      ).out.trim()
      expect(output == message)
    }
  }

  test("main.sc is not a special case") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "main.sc" ->
        s"""println("$message")
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "main.sc").call(cwd =
        root
      ).out.trim()
      expect(output == message)
    }
  }

  test("use method from main.sc file") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "message.sc" ->
        s"""println(main.msg)
           |""".stripMargin,
      os.rel / "main.sc" ->
        s"""def msg = "$message"
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "message.sc", "main.sc").call(cwd =
        root
      ).out.trim()
      expect(output == message)
    }
  }

  test("Directory") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "dir" / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "dir" / "print.sc" ->
        s"""println(messages.msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--main-class", "print_sc").call(cwd =
        root
      ).out.trim()
      expect(output == message)
    }
  }

  test("sub-directory") {
    val fileName          = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath        = os.rel / "something" / fileName
    val inputs = TestInputs(
      scriptPath ->
        s"""println(getClass.getName)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, scriptPath.toString)
        .call(cwd = root)
        .out.text()
        .trim
      expect(output == expectedClassName)
    }
  }

  test("sub-directory and script") {
    val fileName          = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath        = os.rel / "something" / fileName
    val inputs = TestInputs(
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
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "dir", scriptPath.toString)
        .call(cwd = root)
        .out.text()
        .trim
      expect(output == expectedClassName)
    }
  }

  def stackTraceInScriptScala2(): Unit = {
    val inputs = TestInputs(
      os.rel / "throws.sc" ->
        s"""def something(): String =
           |  sys.error("nope")
           |try something()
           |catch {
           |  case e: Exception =>
           |    throw new Exception("Caught exception during processing", e)
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli, "run", extraOptions, ".",
        "--java-prop", "scala.colored-stack-traces=false"
      )
      // format: on
      val res            = os.proc(cmd).call(cwd = root, check = false, mergeErrIntoOut = true)
      val output         = res.out.lines()
      val exceptionLines = output.dropWhile(!_.startsWith("Exception in thread "))
      val tab            = "\t"
      val expectedLines =
        if (actualScalaVersion.startsWith("2.12."))
          s"""Exception in thread "main" java.lang.ExceptionInInitializerError
             |${tab}at throws_sc$$.main(throws.sc:24)
             |${tab}at throws_sc.main(throws.sc)
             |Caused by: java.lang.Exception: Caught exception during processing
             |${tab}at throws$$.<init>(throws.sc:6)
             |${tab}at throws$$.<clinit>(throws.sc)
             |$tab... 2 more
             |Caused by: java.lang.RuntimeException: nope
             |${tab}at scala.sys.package$$.error(package.scala:30)
             |${tab}at throws$$.something(throws.sc:2)
             |${tab}at throws$$.<init>(throws.sc:3)
             |$tab... 3 more""".stripMargin.linesIterator.toVector
        else
          s"""Exception in thread "main" java.lang.ExceptionInInitializerError
             |${tab}at throws_sc$$.main(throws.sc:24)
             |${tab}at throws_sc.main(throws.sc)
             |Caused by: java.lang.Exception: Caught exception during processing
             |${tab}at throws$$.<clinit>(throws.sc:6)
             |$tab... 2 more
             |Caused by: java.lang.RuntimeException: nope
             |${tab}at scala.sys.package$$.error(package.scala:27)
             |${tab}at throws$$.something(throws.sc:2)
             |${tab}at throws$$.<clinit>(throws.sc:3)
             |$tab... 2 more
             |""".stripMargin.linesIterator.toVector
      if (exceptionLines != expectedLines) {
        println(exceptionLines.mkString("\n"))
        println(expectedLines)
      }
      assert(
        exceptionLines.length == expectedLines.length,
        clues(output, exceptionLines.length, expectedLines.length)
      )
      for (i <- exceptionLines.indices)
        assert(
          exceptionLines(i) == expectedLines(i),
          clues(output, exceptionLines(i), expectedLines(i))
        )
    }
  }

  if (actualScalaVersion.startsWith("2."))
    test("stack traces in script") {
      stackTraceInScriptScala2()
    }

  def scriptStackTraceScala3(): Unit = {
    val inputs = TestInputs(
      os.rel / "throws.sc" ->
        s"""def something(): String =
           |  val message = "nope"
           |  sys.error(message)
           |
           |try something()
           |catch {
           |  case e: Exception =>
           |    throw new Exception("Caught exception during processing", e)
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli, "run", extraOptions, ".")
      // format: on
      val res    = os.proc(cmd).call(cwd = root, check = false, mergeErrIntoOut = true)
      val output = res.out.lines()
      val exceptionLines = output
        .map(stripAnsi)
        .dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
        s"""Exception in thread "main" java.lang.ExceptionInInitializerError
           |${tab}at throws_sc$$.main(throws.sc:26)
           |${tab}at throws_sc.main(throws.sc)
           |Caused by: java.lang.Exception: Caught exception during processing
           |${tab}at throws$$.<clinit>(throws.sc:8)
           |$tab... 2 more
           |Caused by: java.lang.RuntimeException: nope
           |${tab}at scala.sys.package$$.error(package.scala:27)
           |${tab}at throws$$.something(throws.sc:3)
           |${tab}at throws$$.<clinit>(throws.sc:5)
           |$tab... 2 more""".stripMargin.linesIterator.toVector
      assert(
        exceptionLines.length == expectedLines.length,
        clues(output, exceptionLines.length, expectedLines.length)
      )
      for (i <- exceptionLines.indices)
        assert(
          exceptionLines(i) == expectedLines(i),
          clues(output, exceptionLines(i), expectedLines(i))
        )
    }
  }

  if (actualScalaVersion.startsWith("3."))
    test("stack traces in script in Scala 3") {
      scriptStackTraceScala3()
    }

  test("pick .scala main class over in-context scripts") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello extends App {
          |  println(s"Hello ${scripts.Script.world}")
          |}
          |""".stripMargin,
      os.rel / "scripts" / "Script.sc" -> """def world: String = "world"""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        "."
      )
        .call(cwd = root)
      expect(res.out.trim() == "Hello world")
    }
  }

  test("interconnection between scripts") {
    val inputs = TestInputs(
      os.rel / "f.sc"     -> "def f(x: String) = println(x + x + x)",
      os.rel / "main0.sc" -> "f.f(args(0))"
    )
    inputs.fromRoot { root =>
      val p =
        os.proc(TestUtil.cli, "main0.sc", "f.sc", "--", "20").call(cwd = root)
      val res = p.out.trim()
      expect(res == "202020")
    }
  }
  test("CLI args passed to script") {
    val inputs = TestInputs(os.rel / "f.sc" -> "println(args(0))")
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "f.sc", "--", "16").call(cwd = root)
      expect(p.out.trim() == "16")
    }
  }

  if (!Properties.isWin)
    test("CLI args passed to shebang script") {
      val inputs = TestInputs(
        os.rel / "f.sc" ->
          s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang -S 2.13
              |//> using scala "$actualScalaVersion"
              |println(args.toList)""".stripMargin
      )
      inputs.fromRoot { root =>
        os.perms.set(root / "f.sc", os.PermSet.fromString("rwx------"))
        val p = os.proc("./f.sc", "1", "2", "3", "-v").call(cwd = root)
        expect(p.out.trim() == "List(1, 2, 3, -v)")
      }
    }
}
