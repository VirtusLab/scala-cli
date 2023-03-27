package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.util.Properties

trait RunScalaNativeTestDefinitions { _: RunTestDefinitions =>
  def simpleNativeTests(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |Zone { implicit z =>
           |   val io = StdioHelpers(stdio)
           |   io.printf(c"%s$platformNl", c"$message")
           |}
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

  test("simple script native") {
    simpleNativeTests()
  }

  def scalaNativeLtoTests(): Unit = {
    val fileName = "hello.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""//> using nativeLto "thin"
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
      scalaNativeLtoTests()
    }

  test("simple script native command") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalanative.libc._
           |import scala.scalanative.unsafe._
           |
           |Zone { implicit z =>
           |   val io = StdioHelpers(stdio)
           |   io.printf(c"%s$platformNl", c"$message")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, fileName, "--native", "--command")
          .call(cwd = root)
          .out.trim()
      val command      = output.linesIterator.toVector.filter(!_.startsWith("["))
      val actualOutput = os.proc(command).call(cwd = root).out.trim()
      expect(actualOutput == message)
    }
  }

  test("Resource embedding in Scala Native") {
    val projectDir       = "nativeres"
    val resourceContent  = "resource contents"
    val resourceFileName = "embeddedfile.txt"
    val inputs = TestInputs(
      os.rel / projectDir / "main.scala" ->
        s"""|//> using platform "scala-native"
            |//> using resourceDir "resources"
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

  test("Scala Native C Files are correctly handled as a regular Input") {
    val projectDir      = "native-interop"
    val interopFileName = "bindings.c"
    val interopMsg      = "Hello C!"
    val inputs = TestInputs(
      os.rel / projectDir / "main.scala" ->
        s"""|//> using platform "scala-native"
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

  if (actualScalaVersion.startsWith("3.2"))
    test("Scala 3 in Scala Native") {
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

  def multipleScriptsNative(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "messages.sc" ->
        s"""def msg = "$message"
           |""".stripMargin,
      os.rel / "print.sc" ->
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
        os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--native", "-q")
          .call(cwd = root)
          .out.trim()
      expect(output == message)
    }
  }

  test("Multiple scripts native") {
    multipleScriptsNative()
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
        os.proc(TestUtil.cli, extraOptions, "dir", "--native", "--main-class", "print_sc", "-q")
          .call(cwd = root)
          .out.trim()
      expect(output == message)
    }
  }

  // TODO: make nice messages that the scenario is unsupported with 2.12
  if (actualScalaVersion.startsWith("2.13"))
    test("Directory native") {
      directoryNative()
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
      val output1 = os.proc(TestUtil.cli, "run", "--native", ".")
        .call(cwd = root, env = configEnv ++ Seq("SCALA_CLI_INTERACTIVE_INPUTS" -> "foo.Main1"))
        .out.lines().last
      expect(output1 == "Hello from Main1")
      val output2 = os.proc(TestUtil.cli, "run", "--native", ".")
        .call(cwd = root, env = configEnv ++ Seq("SCALA_CLI_INTERACTIVE_INPUTS" -> "foo.Main2"))
        .out.lines().last
      expect(output2 == "Hello from Main2")
    }
  }
}
