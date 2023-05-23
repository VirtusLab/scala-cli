package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.normalizeConsoleOutput
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
    val fileName = "script.sc"
    val expectedClassName =
      if (actualScalaVersion.startsWith("3."))
        fileName.stripSuffix(".sc") + "$_"
      else
        fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
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
    val fileName = "script.sc"
    val expectedClassName =
      if (actualScalaVersion.startsWith("3."))
        fileName.stripSuffix(".sc") + "$_"
      else
        fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
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
        s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
           |${tab}at throws$$_.<init>(throws.sc:8)
           |${tab}at throws_sc$$.script(throws.sc:25)
           |${tab}at throws_sc$$.main(throws.sc:29)
           |${tab}at throws_sc.main(throws.sc)
           |Caused by: java.lang.RuntimeException: nope
           |${tab}at scala.sys.package$$.error(package.scala:27)
           |${tab}at throws$$_.something(throws.sc:3)
           |${tab}at throws$$_.<init>(throws.sc:5)
           |$tab... 3 more""".stripMargin.linesIterator.toVector
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
  test("print the name of script") {
    val inputs = TestInputs(os.rel / "hello.sc" -> "println(scriptPath)")
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "hello.sc").call(cwd = root)
      expect(p.out.trim() == "hello.sc")
    }
  }
  test("print the name of nested script") {
    val inputs = TestInputs(os.rel / "dir" / "hello.sc" -> "println(scriptPath)")
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "dir/hello.sc").call(cwd = root)
      expect(p.out.trim() == "dir/hello.sc")
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

  test("script file with shebang header and no extension run with scala-cli shebang") {
    val inputs = TestInputs(
      os.rel / "script-with-shebang" ->
        s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang -S 2.13
            |//> using scala "$actualScalaVersion"
            |println(args.toList)""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = if (!Properties.isWin) {
        os.perms.set(root / "script-with-shebang", os.PermSet.fromString("rwx------"))
        os.proc("./script-with-shebang", "1", "2", "3", "-v").call(cwd = root).out.trim()
      }
      else
        os.proc(TestUtil.cli, "shebang", "script-with-shebang", "1", "2", "3", "-v")
          .call(cwd = root).out.trim()
      expect(output == "List(1, 2, 3, -v)")
    }
  }

  test("script file with NO shebang header and no extension run with scala-cli shebang") {
    val inputs = TestInputs(
      os.rel / "script-no-shebang" ->
        s"""//> using scala "$actualScalaVersion"
           |println(args.toList)""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = if (!Properties.isWin) {
        os.perms.set(root / "script-no-shebang", os.PermSet.fromString("rwx------"))
        os.proc(TestUtil.cli, "shebang", "script-no-shebang", "1", "2", "3", "-v")
          .call(cwd = root, check = false, stderr = os.Pipe).err.trim()
      }
      else
        os.proc(TestUtil.cli, "shebang", "script-no-shebang", "1", "2", "3", "-v")
          .call(cwd = root, check = false, stderr = os.Pipe).err.trim()

      expect(output.contains(
        "unrecognized source type (expected .scala or .sc extension, or a directory)"
      ))

      if (TestUtil.isShebangCapableShell)
        expect(output.contains("shebang header"))
    }
  }

  test("shebang run does not produce update-dependency warnings") {
    val dependencyOsLib = "com.lihaoyi::os-lib:0.7.8"

    val inputs = TestInputs(
      os.rel / "script.sc" ->
        s"""//> using scala "$actualScalaVersion"
           |//> using dep "$dependencyOsLib"
           |
           |println(args.toList)""".stripMargin
    )
    inputs.fromRoot { root =>
      val proc = os.proc(TestUtil.cli, "shebang", "script.sc", "1", "2", "3", "-v")
        .call(cwd = root, mergeErrIntoOut = true)

      expect(!proc.out.text().contains("[hint] \"os-lib is outdated"))
    }
  }

  if (actualScalaVersion.startsWith("3.")) {
    test("no deadlock when running background threads") {
      val inputs = TestInputs(
        os.rel / "script.sc" ->
          s"""//> using scala "$actualScalaVersion"
             |
             |import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}
             |import scala.concurrent.duration._
             |
             |implicit val ec: ExecutionContextExecutor = ExecutionContext.global
             |
             |val future =
             |  for {
             |    message <- Future("Hello world")
             |    _ <- Future(println(message))
             |  } yield ()
             |
             |Await.ready(future, Duration(5, SECONDS))
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        os.proc(TestUtil.cli, "script.sc")
          .call(cwd = root, mergeErrIntoOut = true)
      }
    }

    test("user readable error when @main is used") {
      val inputs = TestInputs(
        os.rel / "script.sc" ->
          """//> using dep "com.lihaoyi::os-lib:0.9.1"
            |/*ignore this while regexing*/ @main def main(args: Strings*): Unit = println("Hello")
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "script.sc")
          .call(cwd = root, mergeErrIntoOut = true, check = false, stdout = os.Pipe)

        val outputNormalized: String = normalizeConsoleOutput(res.out.text())

        expect(outputNormalized.contains("Not found: type Strings"))
        expect(outputNormalized.contains(
          "[warn]  Annotation @main in .sc scripts is not supported, use .scala format instead"
        ))

        val snippetRes = os.proc(
          TestUtil.cli,
          "--script-snippet",
          """@main def main(args: Strings*): Unit = println("Hello")"""
        )
          .call(cwd = root, mergeErrIntoOut = true, check = false, stdout = os.Pipe)

        val snippetOutputNormalized: String = normalizeConsoleOutput(snippetRes.out.text())

        expect(snippetOutputNormalized.contains("Not found: type Strings"))
        expect(snippetOutputNormalized.contains(
          "[warn]  Annotation @main in .sc scripts is not supported, use .scala format instead"
        ))

        val noBloopRes = os.proc(TestUtil.cli, "--server=false", "script.sc")
          .call(cwd = root, mergeErrIntoOut = true, check = false, stdout = os.Pipe)

        val noBloopOutputNormalized: String = normalizeConsoleOutput(noBloopRes.out.text())

        expect(noBloopOutputNormalized.contains(
          "[warn]  Annotation @main in .sc scripts is not supported"
        ))

        val scala2Res = os.proc(TestUtil.cli, "--server=false", "script.sc", "-S", "2")
          .call(cwd = root, mergeErrIntoOut = true, check = false, stdout = os.Pipe)

        val scala2OutputNormalized: String = normalizeConsoleOutput(scala2Res.out.text())

        expect(scala2OutputNormalized.contains(
          "[warn]  Annotation @main in .sc scripts is not supported, it will be ignored, use .scala format instead"
        ))
      }
    }

    test("@main error unchanged in .scala") {
      val inputs = TestInputs(
        os.rel / "main.scala" ->
          """class Main {
            | @main def main(args: String*): Unit = println("Hello")
            |}
            |""".stripMargin,
        os.rel / "script.sc" ->
          """@main def main(args: String*): Unit = println("Hello")"""
      )
      inputs.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "main.scala")
          .call(cwd = root, mergeErrIntoOut = true, check = false)

        expect(!normalizeConsoleOutput(res.out.text())
          .contains("Annotation @main in .sc scripts is not supported"))

        val noBloopRes = os.proc(TestUtil.cli, "--server=false", "main.scala")
          .call(cwd = root, mergeErrIntoOut = true, check = false)

        expect(!normalizeConsoleOutput(noBloopRes.out.text())
          .contains("Annotation @main in .sc scripts is not supported"))

        val noBloopScalaSnippetRes = os.proc(
          TestUtil.cli,
          "--server=false",
          "--scala-snippet",
          "class Main { @main def main(args: Strings*): Unit = println(\"Hello\")}"
        )
          .call(cwd = root, mergeErrIntoOut = true, check = false)

        expect(!normalizeConsoleOutput(noBloopScalaSnippetRes.out.text())
          .contains("Annotation @main in .sc scripts is not supported"))
      }
    }

    test("object-wrapped script forced") {
      val inputs = TestInputs(
        os.rel / "script.sc" ->
          """//> using dep "com.lihaoyi::os-lib:0.9.1"
            |@main def main(args: String*): Unit = println("Hello")
            |""".stripMargin,
        os.rel / "script_with_directive.sc" ->
          """//> using dep "com.lihaoyi::os-lib:0.9.1"
            |//> using objectWrapper
            |@main def main(args: String*): Unit = println("Hello")
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val res = os.proc(TestUtil.cli, "--power", "script.sc", "--object-wrapper")
          .call(cwd = root, mergeErrIntoOut = true, stdout = os.Pipe)

        val outputNormalized: String = normalizeConsoleOutput(res.out.text())

        expect(outputNormalized.contains(
          "[warn]  Annotation @main in .sc scripts is not supported, it will be ignored, use .scala format instead"
        ))
      }
    }
  }
}
