package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.concurrent.duration.DurationInt
import scala.util.{Properties, Try}

trait RunWithWatchTestDefinitions { _: RunTestDefinitions =>
  if (!Properties.isMac || !TestUtil.isCI)
    // TODO make this pass reliably on Mac CI
    test("simple --watch .scala source") {
      val expectedMessage1 = "Hello"
      val inputPath        = os.rel / "smth.scala"
      TestInputs(inputPath -> s"""object Smth extends App { println("$expectedMessage1") }""")
        .fromRoot { root =>
          TestUtil.withProcessWatching(
            proc = os.proc(TestUtil.cli, "run", ".", "--watch", extraOptions)
              .spawn(cwd = root, stderr = os.Pipe),
            timeout = 120.seconds
          ) { (proc, timeout, ec) =>
            val output1 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output1 == expectedMessage1)
            val expectedMessage2 = "World"
            while (!TestUtil.readLine(proc.stderr, ec, timeout).contains("re-run"))
              Thread.sleep(100L)
            os.write.over(
              root / inputPath,
              s"""object Smth extends App { println("$expectedMessage2") }"""
            )
            val output2 = TestUtil.readLine(proc.stdout, ec, timeout)
            expect(output2 == expectedMessage2)
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
      val libSourcePath = os.rel / "lib" / "Messages.scala"
      def libSource(hello: String) =
        s"""//> using publish.organization "test-org"
           |//> using publish.name "messages"
           |//> using publish.version "0.1.0"
           |
           |package messages
           |
           |object Messages {
           |  def hello(name: String) = s"$hello $$name"
           |}
           |""".stripMargin
      TestInputs(
        libSourcePath -> libSource("Hello"),
        os.rel / "app" / "TestApp.scala" ->
          """//> using lib "test-org::messages:0.1.0"
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
        """//> using lib "org.scalameta::munit::0.7.29"
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
}