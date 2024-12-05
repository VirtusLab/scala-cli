package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.util.Properties

trait RunScalaNativeTestDefinitions { _: RunTestDefinitions =>

  def simpleNativeScriptCode(
    message: String,
    wrapMessage: String => String = m => s"c\"$m\""
  ): String =
    if (actualScalaVersion.startsWith("3"))
      s"""import scala.scalanative.libc._
         |import scala.scalanative.unsafe._
         |
         |Zone {
         |   val io = StdioHelpers(stdio)
         |   io.printf(c"%s$platformNl", ${wrapMessage(message)})
         |}
         |""".stripMargin
    else
      s"""import scala.scalanative.libc._
         |import scala.scalanative.unsafe._
         |
         |Zone.acquire { implicit z =>
         |   val io = StdioHelpers(stdio)
         |   io.printf(c"%s$platformNl", ${wrapMessage(message)})
         |}
         |""".stripMargin

  def simpleNativeTests(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    TestInputs(os.rel / fileName -> simpleNativeScriptCode(message))
      .fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, extraOptions, fileName, "--native")
            .call(cwd = root)
            .out.trim()
        expect(output == message)
      }
  }

  test("simple script native") {
    TestUtil.retryOnCi() {
      simpleNativeTests()
    }
  }

  def scalaNativeLtoTests(): Unit = {
    val fileName = "hello.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""//> using nativeLto thin
           |println("$message")
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, fileName, "--native")
          .call(cwd = root)
          .out.trim()
      expect(output == message)
    }
  }

  if (!Properties.isMac)
    test("scala native with lto optimisation") {
      TestUtil.retryOnCi()(scalaNativeLtoTests())
    }

  test("simple script native command") {
    TestUtil.retryOnCi() {
      val fileName = "simple.sc"
      val message  = "Hello"
      TestInputs(os.rel / fileName -> simpleNativeScriptCode(message))
        .fromRoot { root =>
          val output =
            os.proc(TestUtil.cli, extraOptions, fileName, "--native", "--command")
              .call(cwd = root)
              .out.trim()
          val command      = output.linesIterator.toVector.filter(!_.startsWith("["))
          val actualOutput = os.proc(command).call(cwd = root).out.trim()
          expect(actualOutput == message)
        }
    }
  }

  test("Resource embedding in Scala Native") {
    TestUtil.retryOnCi() {
      val projectDir       = "nativeres"
      val resourceContent  = "resource contents"
      val resourceFileName = "embeddedfile.txt"
      val inputs = TestInputs(
        os.rel / projectDir / "main.scala" ->
          s"""|//> using platform scala-native
              |//> using resourceDir resources
              |
              |import java.nio.charset.StandardCharsets
              |import java.io.{BufferedReader, InputStreamReader}
              |
              |object Main {
              |  def main(args: Array[String]): Unit = {
              |    val inputStream = getClass().getResourceAsStream("/$resourceFileName")
              |    val nativeResourceText = new BufferedReader(
              |      new InputStreamReader(inputStream, StandardCharsets.UTF_8)
              |    ).readLine()
              |    println(nativeResourceText)
              |  }
              |}
              |""".stripMargin,
        os.rel / projectDir / "resources" / resourceFileName -> resourceContent
      )
      inputs.fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, extraOptions, projectDir, "-q")
            .call(cwd = root)
            .out.trim()
        println(output)
        expect(output == resourceContent)
      }
    }
  }

  test("Scala Native C Files are correctly handled as a regular Input") {
    TestUtil.retryOnCi() {
      val projectDir      = "native-interop"
      val interopFileName = "bindings.c"
      val interopMsg      = "Hello C!"
      val inputs = TestInputs(
        os.rel / projectDir / "main.scala" ->
          s"""|//> using platform scala-native
              |
              |import scala.scalanative.unsafe._
              |
              |@extern
              |object Bindings {
              |  @name("scalanative_print")
              |  def print(): Unit = extern
              |}
              |
              |object Main {
              |  def main(args: Array[String]): Unit = {
              |    Bindings.print()
              |  }
              |}
              |""".stripMargin,
        os.rel / projectDir / interopFileName ->
          s"""|#include <stdio.h>
              |
              |void scalanative_print() {
              |    printf("$interopMsg\\n");
              |}
              |""".stripMargin
      )
      inputs.fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, extraOptions, projectDir, "-q")
            .call(cwd = root)
            .out.trim()
        expect(output == interopMsg)

        os.move(root / projectDir / interopFileName, root / projectDir / "bindings2.c")
        val output2 =
          os.proc(TestUtil.cli, extraOptions, projectDir, "-q")
            .call(cwd = root)
            .out.trim()

        // LLVM throws linking errors if scalanative_print is internally repeated.
        // This can happen if a file containing it will be removed/renamed in src,
        // but somehow those changes will not be reflected in the output directory,
        // causing symbols inside linked files to be doubled.
        // Because of that, the removed file should not be passed to linker,
        // otherwise this test will fail.
        expect(output2 == interopMsg)
      }
    }
  }

  if (actualScalaVersion.startsWith("3.2"))
    test("Scala 3 in Scala Native") {
      TestUtil.retryOnCi() {
        val message  = "using Scala 3 Native"
        val fileName = "scala3native.scala"
        val inputs = TestInputs(
          os.rel / fileName ->
            s"""import scala.scalanative.libc._
               |import scala.scalanative.unsafe._
               |
               |@main def main() =
               |  val message = "$message"
               |  Zone { implicit z =>
               |    stdio.printf(toCString(message))
               |  }
               |""".stripMargin
        )
        inputs.fromRoot { root =>
          val output =
            os.proc(TestUtil.cli, extraOptions, fileName, "--native", "-q")
              .call(cwd = root)
              .out.trim()
          expect(output == message)
        }
      }
    }

  def multipleScriptsNative(): Unit = {
    val message = "Hello"
    TestInputs(os.rel / "print.sc" ->
      simpleNativeScriptCode("messages.msg", m => s"toCString($m)"))
      .add(os.rel / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin)
      .fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--native")
            .call(cwd = root)
            .out.trim()
        expect(output == message)
      }
  }

  test("Multiple scripts native") {
    TestUtil.retryOnCi()(multipleScriptsNative())
  }

  def directoryNative(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "dir" / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "dir" / "print.sc" ->
        s"""import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |Zone { implicit z =>
           |   val io = StdioHelpers(stdio)
           |   io.printf(c"%s$platformNl", toCString(messages.msg))
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, "dir", "--native", "--main-class", "print", "-q")
          .call(cwd = root)
          .out.trim()
      expect(output == message)
    }
  }

  // TODO: make nice messages that the scenario is unsupported with 2.12
  if (actualScalaVersion.startsWith("2.13"))
    test("Directory native") {
      TestUtil.retryOnCi()(directoryNative())
    }

  test("help native") {
    val helpNativeOption  = "--help-native"
    val helpNative        = os.proc(TestUtil.cli, "run", helpNativeOption).call(check = false)
    val lines             = removeAnsiColors(helpNative.out.trim()).linesIterator.toVector
    val nativeVersionHelp = lines.find(_.contains("--native-version")).getOrElse("")
    expect(nativeVersionHelp.contains(s"(${Constants.scalaNativeVersion} by default)"))
    expect(lines.exists(_.contains("Scala Native options")))
    expect(!lines.exists(_.contains("Scala.js options")))
  }

  test("Take into account interactive main class when caching binaries") {
    TestUtil.retryOnCi() {
      val inputs = TestInputs(
        os.rel / "Main1.scala" ->
          """package foo
            |
            |object Main1 {
            |  def main(args: Array[String]): Unit =
            |    println("Hello from Main1")
            |}
            |""".stripMargin,
        os.rel / "Main2.scala" ->
          """package foo
            |
            |object Main2 {
            |  def main(args: Array[String]): Unit =
            |    println("Hello from Main2")
            |}
            |""".stripMargin
      )
      inputs.fromRoot { root =>
        val configDir = root / "config"
        os.makeDir.all(configDir)
        if (!Properties.isWin)
          os.perms.set(configDir, "rwx------")
        val configEnv = Map("SCALA_CLI_CONFIG" -> (configDir / "config.json").toString)
        os.proc(TestUtil.cli, "config", "interactive", "true")
          .call(cwd = root, env = configEnv)
        val output1 = os.proc(TestUtil.cli, "run", "--native", ".", extraOptions)
          .call(cwd = root, env = configEnv ++ Seq("SCALA_CLI_INTERACTIVE_INPUTS" -> "foo.Main1"))
          .out.lines().last
        expect(output1 == "Hello from Main1")
        val output2 = os.proc(TestUtil.cli, "run", "--native", ".", extraOptions)
          .call(cwd = root, env = configEnv ++ Seq("SCALA_CLI_INTERACTIVE_INPUTS" -> "foo.Main2"))
          .out.lines().last
        expect(output2 == "Hello from Main2")
      }
    }
  }

  if (!actualScalaVersion.startsWith("2.12"))
    for {
      useDirectives <- Seq(true, false)
      titleStr = if (useDirectives) "with directives" else "with command line args"
      explicitNativeVersion <-
        Seq(Some(Constants.scalaNativeVersion04), Some(Constants.scalaNativeVersion05), None)
      actualNativeVersion = explicitNativeVersion.getOrElse(Constants.scalaNativeVersion)
      nativeVersionStr    = explicitNativeVersion.map(v => s"explicit: $v").getOrElse("default")
      nativeVersionOpts   = explicitNativeVersion.toSeq.flatMap(v => Seq("--native-version", v))
      nativeVersionDirectiveStr =
        explicitNativeVersion
          .map(v => s"""//> using nativeVersion $v
                       |""".stripMargin)
          .getOrElse("")
      scalaToolkitVersion =
        if (explicitNativeVersion.contains(Constants.scalaNativeVersion04))
          Constants.toolkitVersionForNative04
        else "default"
      typelevelToolkitVersion = "default"
    } {
      // for the time being, typelevel toolkit does nto support Scala Native 0.5.x
      if (!explicitNativeVersion.contains(Constants.scalaNativeVersion05))
        test(
          s"native ($nativeVersionStr) & typelevel toolkit ($typelevelToolkitVersion) $titleStr"
        ) {
          TestUtil.retryOnCi() {
            val expectedMessage = "Hello"
            val cmdLineOpts =
              if (useDirectives) Nil
              else Seq(
                "--toolkit",
                s"typelevel:$typelevelToolkitVersion",
                "--native"
              ) ++ nativeVersionOpts
            val directivesStr =
              if (useDirectives)
                s"""//> using toolkit typelevel:$typelevelToolkitVersion
                   |//> using platform native
                   |$nativeVersionDirectiveStr
                   |""".stripMargin
              else ""
            TestInputs(
              os.rel / "toolkit.scala" ->
                s"""$directivesStr
                   |import cats.effect._
                   |
                   |object Hello extends IOApp.Simple {
                   |  def run =  IO.println("$expectedMessage")
                   |}
                   |""".stripMargin
            ).fromRoot { root =>
              val result = os.proc(TestUtil.cli, "run", "toolkit.scala", cmdLineOpts, extraOptions)
                .call(cwd = root, stderr = os.Pipe)
              expect(result.out.trim() == expectedMessage)
              if (actualNativeVersion != Constants.typelevelToolkitMaxScalaNative) {
                val err = result.err.trim()
                expect(
                  err.contains(
                    s"Scala Native default version ${Constants.scalaNativeVersion} is not supported in this build"
                  )
                )
                expect(err.contains(s"Using ${Constants.typelevelToolkitMaxScalaNative} instead."))
                expect(err.contains(
                  s"TypeLevel Toolkit ${Constants.typelevelToolkitVersion} does not support Scala Native ${Constants.scalaNativeVersion}"
                ))
              }
            }
          }
        }

      test(s"native ($nativeVersionStr) & scala toolkit ($scalaToolkitVersion) $titleStr") {
        TestUtil.retryOnCi() {
          val cmdLineOpts =
            if (useDirectives) Nil
            else Seq("--toolkit", scalaToolkitVersion, "--native") ++ nativeVersionOpts
          val directivesStr =
            if (useDirectives)
              s"""//> using toolkit $scalaToolkitVersion
                 |//> using platform native
                 |$nativeVersionDirectiveStr
                 |""".stripMargin
            else ""
          TestInputs(
            os.rel / "toolkit.scala" ->
              s"""$directivesStr
                 |object Hello extends App {
                 |  println(os.pwd)
                 |}
                 |""".stripMargin
          ).fromRoot { root =>
            val result = os.proc(TestUtil.cli, "run", "toolkit.scala", cmdLineOpts, extraOptions)
              .call(cwd = root, stderr = os.Pipe)
            expect(result.out.trim() == root.toString)
            if (Constants.scalaNativeVersion != Constants.toolkiMaxScalaNative) {
              val err = result.err.trim()
              expect(
                err.contains(
                  s"Scala Native default version ${Constants.scalaNativeVersion} is not supported in this build"
                )
              )
              expect(err.contains(s"Using ${Constants.toolkiMaxScalaNative} instead."))
              expect(err.contains(
                s"Scala Toolkit does not support Scala Native ${Constants.scalaNativeVersion}"
              ))
            }
          }
        }
      }
    }

  for {
    expectedMultithreadingState <- Seq(true, false)
    // multithreading should be enabled by Scala Native by default
    setExplicitly <- if (expectedMultithreadingState) Seq(true, false) else Seq(true)
    useDirective  <- if (setExplicitly) Seq(true, false) else Seq(false)
    directive =
      if (useDirective && setExplicitly)
        s"//> using nativeMultithreading $expectedMultithreadingState"
      else ""
    cliOptions =
      if (!useDirective && setExplicitly)
        Seq(s"--native-multithreading=$expectedMultithreadingState")
      else Nil
    testDescriptionString = useDirective -> setExplicitly match {
      case (_, false)    => "(implicitly)"
      case (true, true)  => "with directive"
      case (false, true) => "with command line option"
    }
  }
    test(
      s"Scala Native multithreading set to $expectedMultithreadingState $testDescriptionString"
    ) {
      TestUtil.retryOnCi() {
        val fileName       = "multithreading.sc"
        val expectedOutput = "42"
        val threadSleep    = "100"
        val threadAwait    = "2.seconds"
        val inputs = TestInputs(
          os.rel / fileName ->
            s"""$directive
               |import scala.concurrent._
               |import scala.concurrent.duration._
               |import ExecutionContext.Implicits.global
               |val promise = Promise[Int]()
               |val thread = new Thread(new Runnable {
               |    def run(): Unit = {
               |      Thread.sleep($threadSleep)
               |      promise.success($expectedOutput)
               |    }
               |  })
               |thread.start()
               |val result = Await.result(promise.future, $threadAwait)
               |println(result)
               |""".stripMargin
        )
        inputs.fromRoot { root =>
          val r = os.proc(TestUtil.cli, extraOptions, fileName, "--native", cliOptions)
            .call(cwd = root, stderr = os.Pipe, check = expectedMultithreadingState)
          if (!expectedMultithreadingState) expect(r.exitCode == 1)
          else {
            expect(r.exitCode == 0)
            expect(r.out.trim() == expectedOutput)
          }
          expect(r.err.trim().contains(s"multithreadingEnabled=$expectedMultithreadingState"))
        }
      }
    }
}
