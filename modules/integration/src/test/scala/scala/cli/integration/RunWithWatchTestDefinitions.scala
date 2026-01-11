package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.annotation.tailrec
import scala.cli.integration.TestUtil.ProcOps
import scala.concurrent.duration.DurationInt
import scala.util.{Properties, Try}

trait RunWithWatchTestDefinitions { this: RunTestDefinitions =>
  // TODO make this pass reliably on Mac CI
  if (!Properties.isMac || !TestUtil.isCI) {
    val expectedMessage1 = "Hello"
    val expectedMessage2 = "World"
    for {
      (inputPath, inputs, codeToWriteOver) <-
        Seq(
          {
            val inputPath             = os.rel / "raw.scala"
            def code(message: String) = s"""object Smth extends App { println("$message") }"""
            (
              inputPath,
              TestInputs(inputPath -> code(expectedMessage1)),
              code(expectedMessage2)
            )
          }, {
            val inputPath             = os.rel / "script.sc"
            def code(message: String) = s"""println("$message")"""
            (
              inputPath,
              TestInputs(inputPath -> code(expectedMessage1)),
              code(expectedMessage2)
            )
          }, {
            val inputPath             = os.rel / "Main.java"
            def code(message: String) = s"""public class Main {
                                           |  public static void main(String[] args) {
                                           |    System.out.println("$message");
                                           |  }
                                           |}""".stripMargin
            (
              inputPath,
              TestInputs(inputPath -> code(expectedMessage1)),
              code(expectedMessage2)
            )
          }, {
            val inputPath             = os.rel / "markdown.md"
            def code(message: String) =
              s"""# Some random docs with a Scala snippet
                 |```scala
                 |println("$message")
                 |```
                 |The snippet prints the message, of course.
                 |""".stripMargin
            (
              inputPath,
              TestInputs(inputPath -> code(expectedMessage1)),
              code(expectedMessage2)
            )
          }
        )
    }
      test(s"simple --watch ${inputPath.last}") {
        inputs.fromRoot { root =>
          TestUtil.withProcessWatching(
            proc = os.proc(TestUtil.cli, "run", inputPath.toString(), "--watch", extraOptions)
              .spawn(cwd = root, stderr = os.Pipe),
            timeout = 120.seconds
          ) { (proc, timeout, ec) =>
            val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output1 == expectedMessage1)
            proc.printStderrUntilRerun(timeout)(ec)
            os.write.over(root / inputPath, codeToWriteOver)
            val output2 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output2 == expectedMessage2)
          }
        }
      }
  }

  for {
    (platformDescription, platformOpts) <- Seq(
      "JVM"    -> Nil,
      "JS"     -> Seq("--js"),
      "Native" -> Seq("--native")
    )
    // TODO make this pass reliably on Mac CI https://github.com/VirtusLab/scala-cli/issues/2517
    if !Properties.isMac || !TestUtil.isCI
  }
    test(s"--watch --test ($platformDescription)") {
      TestUtil.retryOnCi() {
        val expectedMessage1 = "Hello from the test scope 1"
        val expectedMessage2 = "Hello from the test scope 2"
        val inputPath        = os.rel / "example.test.scala"

        def code(expectedMessage: String) =
          s"""object Main extends App { println("$expectedMessage") }"""

        TestInputs(
          inputPath -> code(expectedMessage1)
        ).fromRoot { root =>
          TestUtil.withProcessWatching(
            proc =
              os.proc(
                TestUtil.cli,
                "run",
                inputPath.toString(),
                "--watch",
                "--test",
                extraOptions,
                platformOpts
              )
                .spawn(cwd = root, stderr = os.Pipe),
            timeout = 300.seconds
          ) { (proc, timeout, ec) =>
            val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output1 == expectedMessage1)
            proc.printStderrUntilRerun(timeout)(ec)
            os.write.over(root / inputPath, code(expectedMessage2))
            val output2 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output2 == expectedMessage2)
          }
        }
      }
    }

  test("watch with interactive, with multiple main classes") {
    val fileName = "watch.scala"
    TestInputs(
      os.rel / fileName ->
        """object Run1 extends App {println("Run1 launched")}
          |object Run2 extends App {println("Run2 launched")}
          |""".stripMargin
    ).fromRoot { root =>
      val confDir  = root / "config"
      val confFile = confDir / "test-config.json"

      os.write(confFile, "{}", createFolders = true)

      if (!Properties.isWin)
        os.perms.set(confDir, "rwx------")

      val configEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      TestUtil.withProcessWatching(
        proc = os.proc(TestUtil.cli, "run", "--watch", "--interactive", fileName)
          .spawn(
            cwd = root,
            mergeErrIntoOut = true,
            stdout = os.Pipe,
            stdin = os.Pipe,
            env = Map("SCALA_CLI_INTERACTIVE" -> "true") ++ configEnv
          ),
        timeout = 60.seconds
      ) { (proc, timeout, ec) =>
        def lineReaderIter: Iterator[String] = Iterator.continually {
          val line = TestUtil.readLine(proc.stdout, ec, timeout)
          println(s"Line read: $line")
          line
        }

        def checkLinesForError(lines: Seq[String]): Unit = munit.Assertions.assert(
          !lines.exists { line =>
            TestUtil.removeAnsiColors(line).contains("[error]")
          },
          clues(lines.toSeq)
        )

        def answerInteractivePrompt(id: Int): Unit = {
          val interactivePromptLines = lineReaderIter
            .takeWhile(!_.startsWith("[1]" /* probably [1] Run2  or [1] No*/ ))
            .toList
          expect(interactivePromptLines.nonEmpty)
          checkLinesForError(interactivePromptLines)
          proc.stdin.write(s"$id\n")
          proc.stdin.flush()
        }

        def analyzeRunOutput(restart: Boolean): Unit = {
          val runResultLines = lineReaderIter
            .takeWhile(!_.contains("press Enter to re-run"))
            .toList
          expect(runResultLines.nonEmpty)
          checkLinesForError(runResultLines)
          if (restart)
            proc.stdin.write("\n")
          proc.stdin.flush()
        }

        // You have run the current scala-cli command with the --interactive mode turned on.
        // Would you like to leave it on permanently?
        answerInteractivePrompt(0)

        // Found several main classes. Which would you like to run?
        answerInteractivePrompt(0)
        expect(TestUtil.readLine(proc.stdout, ec, timeout) == "Run1 launched")

        analyzeRunOutput( /* restart */ true)

        answerInteractivePrompt(1)
        expect(TestUtil.readLine(proc.stdout, ec, timeout) == "Run2 launched")

        analyzeRunOutput( /* restart */ false)
      }
    }
  }

  if (!Properties.isMac || !TestUtil.isNativeCli || !TestUtil.isCI)
    // TODO make this pass reliably on Mac CI
    test("watch artifacts") {
      val libSourcePath            = os.rel / "lib" / "Messages.scala"
      def libSource(hello: String) =
        s"""//> using publish.organization test-org
           |//> using publish.name messages
           |//> using publish.version 0.1.0
           |
           |package messages
           |
           |object Messages {
           |  def hello(name: String) = s"$hello $$name"
           |}
           |""".stripMargin
      TestInputs(
        libSourcePath                    -> libSource("Hello"),
        os.rel / "app" / "TestApp.scala" ->
          """//> using lib test-org::messages:0.1.0
            |
            |package testapp
            |
            |import messages.Messages
            |
            |@main
            |def run(): Unit =
            |  println(Messages.hello("user"))
            |""".stripMargin
      ).fromRoot { root =>
        val testRepo = root / "test-repo"

        def publishLib(): Unit =
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            "--offline",
            "--publish-repo",
            testRepo,
            "lib"
          )
            .call(cwd = root)

        publishLib()

        TestUtil.withProcessWatching(
          os.proc(
            TestUtil.cli,
            "--power",
            "run",
            "--offline",
            "app",
            "-w",
            "-r",
            testRepo.toNIO.toUri.toASCIIString
          ).spawn(cwd = root)
        ) { (proc, timeout, ec) =>
          val output = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output == "Hello user")

          os.write.over(root / libSourcePath, libSource("Hola"))
          publishLib()

          val secondOutput = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(secondOutput == "Hola user")
        }
      }
    }

  test("watch test - no infinite loop") {
    val fileName = "watch.scala"
    TestInputs(
      os.rel / fileName ->
        """//> using dep org.scalameta::munit::0.7.29
          |
          |class MyTests extends munit.FunSuite {
          |    test("is true true") { assert(true) }
          |}
          |""".stripMargin
    ).fromRoot { root =>
      TestUtil.withProcessWatching(
        proc = os.proc(TestUtil.cli, "test", "-w", "watch.scala")
          .spawn(cwd = root, mergeErrIntoOut = true),
        timeout = 10.seconds
      ) { (proc, timeout, ec) =>
        val watchingMsg = "Watching sources, press Ctrl+C to exit, or press Enter to re-run."
        val testingMsg  = "MyTests:"

        def lineReadIter = Iterator.continually(Try(TestUtil.readLine(proc.stdout, ec, timeout)))
          .takeWhile(_.isSuccess)
          .map(_.get)

        val beforeAppendOut = lineReadIter.toSeq
        expect(beforeAppendOut.count(_.contains(testingMsg)) == 1)
        expect(beforeAppendOut.count(_.contains(watchingMsg)) == 1)
        expect(beforeAppendOut.last.contains(watchingMsg))

        os.write.append(root / fileName, "\n//comment")

        val afterAppendOut = lineReadIter.toSeq
        expect(afterAppendOut.count(_.contains(testingMsg)) == 1)
        expect(afterAppendOut.count(_.contains(watchingMsg)) == 1)
        expect(afterAppendOut.last.contains(watchingMsg))
      }
    }
  }

  // TODO make this pass reliably on Mac CI
  if (!Properties.isMac || !TestUtil.isCI)
    test("--watch .scala source with changing directives") {
      val inputPath = os.rel / "smth.scala"

      def code(includeDirective: Boolean) = {
        val directive = if (includeDirective) "//> using toolkit default" else ""
        s"""$directive
           |object Smth extends App { println(os.pwd) }
           |""".stripMargin
      }

      TestInputs(inputPath -> code(includeDirective = true)).fromRoot { root =>
        TestUtil.withProcessWatching(
          os.proc(TestUtil.cli, "run", ".", "--watch", extraOptions)
            .spawn(cwd = root, stderr = os.Pipe)
        ) { (proc, timeout, ec) =>
          val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output1 == root.toString)
          proc.printStderrUntilRerun(timeout)(ec)
          os.write.over(root / inputPath, code(includeDirective = false))
          TestUtil.readLine(proc.stderr, ec, timeout)
          val output2 = TestUtil.readLine(proc.stderr, ec, timeout)
          expect(output2.toLowerCase.contains("error"))
          proc.printStderrUntilRerun(timeout)(ec)
          os.write.over(root / inputPath, code(includeDirective = true))
          val output3 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output3 == root.toString)
        }
      }
    }

  for {
    useDirective <- Seq(false, true)
    testScope    <- if (useDirective) Seq(false, true) else Seq(false)
    scopeString = if (testScope) "test" else "main"
    // TODO make this pass reliably on Mac CI
    if !Properties.isMac || !TestUtil.isCI
    directive =
      useDirective -> testScope match {
        case (true, true)  => "//> using test.resourceDirs ./resources"
        case (true, false) => "//> using resourceDirs ./resources"
        case _             => ""
      }
    resourceOptions = if (useDirective) Nil else Seq("--resource-dirs", "./src/proj/resources")
    scopeOptions    = if (testScope) Seq("--test") else Nil
    title           = if (useDirective) "directive" else "command line"
  } test(s"resources via $title with --watch ($scopeString)") {
    val expectedMessage1 = "Hello"
    val expectedMessage2 = "world"
    resourcesInputs(directive = directive, resourceContent = expectedMessage1)
      .fromRoot { root =>
        TestUtil.withProcessWatching(
          os.proc(
            TestUtil.cli,
            "run",
            "src",
            "--watch",
            resourceOptions,
            scopeOptions,
            extraOptions
          )
            .spawn(cwd = root, stderr = os.Pipe)
        ) { (proc, timeout, ec) =>
          val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output1 == expectedMessage1)
          proc.printStderrUntilRerun(timeout)(ec)
          val (resourcePath, newResourceContent) =
            resourcesInputs(directive = directive, resourceContent = expectedMessage2)
              .files
              .find(_._1.toString.contains("resources"))
              .get
          os.write.over(root / resourcePath, newResourceContent)
          val output2 = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(output2 == expectedMessage2)
        }
      }
  }

  def testRepeatedRerunsWithWatch(): Unit = {
    def expectedMessage(number: Int) = s"Hello $number"

    def content(number: Int) =
      s"@main def main(): Unit = println(\"${expectedMessage(number)}\")"

    TestUtil.retryOnCi() {
      val inputPath = os.rel / "example.scala"
      TestInputs(inputPath -> content(0)).fromRoot { root =>
        os.proc(TestUtil.cli, "--power", "bloop", "exit").call(cwd = root)
        TestUtil.withProcessWatching(
          proc = os.proc(TestUtil.cli, ".", "--watch", extraOptions)
            .spawn(cwd = root, stderr = os.Pipe)
        ) { (proc, timeout, ec) =>
          for (num <- 1 to 10) {
            val output = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output == expectedMessage(num - 1))
            proc.printStderrUntilRerun(timeout)(ec)
            Thread.sleep(200L)
            if (num < 10) {
              val newContent = content(num)
              os.write.over(root / inputPath, newContent)
            }
          }
        }
      }
    }
  }

  if (actualScalaVersion.startsWith("3") && Properties.isMac && TestUtil.isCI)
    // TODO make this pass reliably on Mac CI (seems to work fine locally)
    test("watch mode doesnt hang on Bloop when rebuilding repeatedly".flaky) {
      testRepeatedRerunsWithWatch()
    }
  else if (actualScalaVersion.startsWith("3"))
    test("watch mode doesnt hang on Bloop when rebuilding repeatedly") {
      testRepeatedRerunsWithWatch()
    }

  // TODO make this pass reliably on Mac CI
  if (!Properties.isMac || !TestUtil.isCI)
    test("--watch with --watch-clear-screen clears screen on rerun") {
      val expectedMessage1 = "Hello1"
      val expectedMessage2 = "Hello2"
      val inputPath        = os.rel / "example.scala"

      def code(message: String) = s"""object Example extends App { println("$message") }"""

      TestInputs(inputPath -> code(expectedMessage1)).fromRoot { root =>
        TestUtil.withProcessWatching(
          proc = os.proc(
            TestUtil.cli,
            "run",
            inputPath.toString(),
            "--watch",
            "--watch-clear-screen",
            extraOptions
          )
            .spawn(cwd = root, mergeErrIntoOut = true),
          timeout = 120.seconds
        ) { (proc, timeout, ec) =>
          def readLine(): String = TestUtil.readLine(proc.stdout, ec, timeout)
          @tailrec
          def readNextStableLine(): String = {
            val line = readLine()
            if (line.contains("Compiling project") || line.contains("Compiled project"))
              readNextStableLine()
            else line
          }
          val output1 = readNextStableLine()
          expect(output1 == expectedMessage1)
          var line = readLine()
          while (!line.contains("Watching sources"))
            line = readLine()
          Thread.sleep(1000)
          os.write.over(root / inputPath, code(expectedMessage2))
          line = readLine()
          while (!line.contains(expectedMessage2) && !line.contains("\u001b[2J"))
            line = readLine()
          while (!line.contains(expectedMessage2))
            line = readLine()
          expect(line.contains(expectedMessage2))
        }
      }
    }

  if (!Properties.isMac || !TestUtil.isCI)
    test("compile --watch with --watch-clear-screen clears screen on rerun") {
      val inputPath = os.rel / "example.scala"
      def code      = s"""object Example extends App { println("Hello") }"""

      TestInputs(inputPath -> code).fromRoot { root =>
        TestUtil.withProcessWatching(
          proc = os.proc(
            TestUtil.cli,
            "compile",
            inputPath.toString(),
            "--watch",
            "--watch-clear-screen",
            extraOptions
          )
            .spawn(cwd = root, mergeErrIntoOut = true),
          timeout = 120.seconds
        ) { (proc, timeout, ec) =>
          def readLine(): String = TestUtil.readLine(proc.stdout, ec, timeout)
          @tailrec
          def readNextStableLine(): String = {
            val line = readLine()
            if (line.contains("Compiling project") || line.contains("Compiled project"))
              readNextStableLine()
            else line
          }
          // First run
          var line = readLine()
          while (!line.contains("Watching sources"))
            line = readLine()

          Thread.sleep(1000)
          os.write.append(root / inputPath, "\n// comment")

          line = readLine()
          // We expect the clear screen escape code to be present before the next "Compiling project" or "Watching sources"
          while (!line.contains("\u001b[2J") && !line.contains("Watching sources"))
            line = readLine()

          expect(line.contains("\u001b[2J"))
        }
      }
    }

  if (!Properties.isMac || !TestUtil.isCI)
    test("test --watch with --watch-clear-screen clears screen on rerun") {
      val inputPath             = os.rel / "example.test.scala"
      def code(message: String) =
        s"""//> using dep org.scalameta::munit::0.7.29
           |class MyTests extends munit.FunSuite {
           |    test("test") { println("$message"); assert(true) }
           |}
           |""".stripMargin

      TestInputs(inputPath -> code("Hello1")).fromRoot { root =>
        TestUtil.withProcessWatching(
          proc = os.proc(
            TestUtil.cli,
            "test",
            inputPath.toString(),
            "--watch",
            "--watch-clear-screen",
            extraOptions
          )
            .spawn(cwd = root, mergeErrIntoOut = true),
          timeout = 120.seconds
        ) { (proc, timeout, ec) =>
          def readLine(): String = TestUtil.readLine(proc.stdout, ec, timeout)
          @tailrec
          def readNextStableLine(): String = {
            val line = readLine()
            if (line.contains("Compiling project") || line.contains("Compiled project"))
              readNextStableLine()
            else line
          }

          // Wait for first run to finish
          var line = readLine()
          while (!line.contains("Watching sources"))
            line = readLine()

          Thread.sleep(1000)
          os.write.over(root / inputPath, code("Hello2"))

          line = readLine()
          // We expect the clear screen escape code to be present
          while (!line.contains("\u001b[2J") && !line.contains("Hello2"))
            line = readLine()

          expect(line.contains("\u001b[2J"))
        }
      }
    }
}
