package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.Charset

import scala.cli.integration.util.DockerServer
import scala.io.Codec
import scala.util.Properties

abstract class RunTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  protected val ciOpt: Seq[String] =
    Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)

  def simpleScriptTest(ignoreErrors: Boolean = false, extraArgs: Seq[String] = Nil): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""val msg = "$message"
             |println(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, extraOptions, extraArgs, fileName).call(cwd = root).out.text().trim
      if (!ignoreErrors)
        expect(output == message)
    }
  }

  // warm-up run that downloads compiler bridges
  // The "Downloading compiler-bridge (from bloop?) pollute the output, and would make the first test fail.
  lazy val warmupTest: Unit = {
    System.err.println("Running RunTests warmup test…")
    simpleScriptTest(ignoreErrors = true)
    System.err.println("Done running RunTests warmup test.")
  }

  override def test(name: String)(body: => Any)(implicit loc: munit.Location): Unit =
    super.test(name) { warmupTest; body }(loc)

  test("simple script") {
    simpleScriptTest()
  }

  test("verbosity") {
    simpleScriptTest(extraArgs = Seq("-v"))
  }

  def simpleJsTest(extraArgs: String*): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
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
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js", extraArgs).call(cwd =
        root
      ).out.text().trim
      expect(output.linesIterator.toSeq.last == message)
    }
  }

  test("simple script JS") {
    simpleJsTest()
  }
  test("simple script JS in release mode") {
    simpleJsTest("--js-mode", "release")
  }

  test("esmodule import JS") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
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
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js")
        .call(cwd = root).out.text().trim()
      expect(output == message)
    }
  }

  test("simple script JS via config file") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""//> using platform "scala-js"
             |import scala.scalajs.js
             |val console = js.Dynamic.global.console
             |val msg = "$message"
             |console.log(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  def platformNl: String = if (Properties.isWin) "\\r\\n" else "\\n"

  def canRunScWithNative: Boolean =
    !(actualScalaVersion.startsWith("2.12") || actualScalaVersion.startsWith("3.0"))

  def simpleNativeTests(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
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
      val output =
        os.proc(TestUtil.cli, extraOptions, fileName, "--native", "-q")
          .call(cwd = root)
          .out.text().trim
      expect(output == message)
    }
  }

  if (canRunScWithNative)
    test("simple script native") {
      simpleNativeTests()
    }
  else
    test("Descriptive error message for unsupported native/script configurations") {
      val inputs = TestInputs(
        Seq(
          os.rel / "a.sc" -> "println(1)"
        )
      )
      val nativeVersion = "0.4.2"
      inputs.fromRoot { root =>
        val output = os.proc(
          TestUtil.cli,
          extraOptions,
          "--native",
          "a.sc",
          "--native-version",
          nativeVersion
        ).call(
          cwd = root,
          check = false,
          stderr = os.Pipe
        ).err.text().trim
        expect(
          output.contains(
            s"Used Scala Native version $nativeVersion is incompatible with Scala $actualScalaVersion."
          )
        )
      }
    }

  if (actualScalaVersion.startsWith("3.1"))
    test("Scala 3 in Scala Native") {
      val message  = "using Scala 3 Native"
      val fileName = "scala3native.scala"
      val inputs = TestInputs(
        Seq(
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
      )
      inputs.fromRoot { root =>
        val output =
          os.proc(TestUtil.cli, extraOptions, fileName, "--native", "-q")
            .call(cwd = root)
            .out.text().trim
        expect(output == message)
      }
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc").call(cwd =
        root
      ).out.text().trim
      expect(output == message)
    }
  }

  test("main.sc is not a special case") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "main.sc" ->
          s"""println("$message")
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "main.sc").call(cwd =
        root
      ).out.text().trim
      expect(output == message)
    }
  }

  test("use method from main.sc file") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "message.sc" ->
          s"""println(main.msg)
             |""".stripMargin,
        os.rel / "main.sc" ->
          s"""def msg = "$message"
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "message.sc", "main.sc").call(cwd =
        root
      ).out.text().trim
      expect(output == message)
    }
  }

  test("Multiple scripts JS") {
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--js").call(cwd =
        root
      ).out.text().trim
      expect(output == message)
    }
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
      val output =
        os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--native", "-q")
          .call(cwd = root)
          .out.text().trim
      expect(output == message)
    }
  }

  if (canRunScWithNative)
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--main-class", "print_sc").call(cwd =
        root
      ).out.text().trim
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
      val res = os.proc(TestUtil.cli, extraOptions, "--main-class", "print")
        .call(cwd = root / "dir", check = false, mergeErrIntoOut = true)
      val output = res.out.text().trim
      expect(res.exitCode != 0)
      expect(output.contains("No inputs provided"))
    }
  }

  test("Pass arguments") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" ->
          s"""object Test {
             |  def main(args: Array[String]): Unit = {
             |    println(args(0))
             |  }
             |}
             |""".stripMargin
      )
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, ".", "--", message)
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  def passArgumentsScala3(): Unit = {
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" ->
          s"""object Test:
             |  def main(args: Array[String]): Unit =
             |    val message = args(0)
             |    println(message)
             |""".stripMargin
      )
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, ".", "--", message)
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  if (actualScalaVersion.startsWith("3."))
    test("Pass arguments - Scala 3") {
      passArgumentsScala3()
    }

  test("Directory JS") {
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--js", "--main-class", "print_sc")
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
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
      val output =
        os.proc(TestUtil.cli, extraOptions, "dir", "--native", "--main-class", "print_sc", "-q")
          .call(cwd = root)
          .out.text().trim
      expect(output == message)
    }
  }

  // TODO: make nice messages that the scenario is unsupported with 2.12
  if (actualScalaVersion.startsWith("2.13"))
    test("Directory native") {
      directoryNative()
    }

  test("sub-directory") {
    val fileName          = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath        = os.rel / "something" / fileName
    val inputs = TestInputs(
      Seq(
        scriptPath ->
          s"""println(getClass.getName)
             |""".stripMargin
      )
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", scriptPath.toString)
        .call(cwd = root)
        .out.text()
        .trim
      expect(output == expectedClassName)
    }
  }

  private lazy val ansiRegex = "\u001B\\[[;\\d]*m".r
  private def stripAnsi(s: String): String =
    ansiRegex.replaceAllIn(s, "")

  test("stack traces") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Throws.scala" ->
          s"""object Throws {
             |  def something(): String =
             |    sys.error("nope")
             |  def main(args: Array[String]): Unit =
             |    try something()
             |    catch {
             |      case e: Exception =>
             |        throw new Exception("Caught exception during processing", e)
             |    }
             |}
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli, "run", extraOptions, ".",
        "--java-prop", "scala.colored-stack-traces=false"
      )
      // format: on
      val res    = os.proc(cmd).call(cwd = root, check = false, mergeErrIntoOut = true)
      val output = res.out.lines()
      // FIXME We need to have the pretty-stacktraces stuff take scala.colored-stack-traces into account
      val exceptionLines =
        output.map(stripAnsi).dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"

      val expectedLines =
        if (actualScalaVersion.startsWith("2.12."))
          s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
             |${tab}at Throws$$.main(Throws.scala:8)
             |${tab}at Throws.main(Throws.scala)
             |Caused by: java.lang.RuntimeException: nope
             |${tab}at scala.sys.package$$.error(package.scala:30)
             |${tab}at Throws$$.something(Throws.scala:3)
             |${tab}at Throws$$.main(Throws.scala:5)
             |$tab... 1 more
             |""".stripMargin.linesIterator.toVector
        else if (actualScalaVersion.startsWith("3.") || actualScalaVersion.startsWith("2.13."))
          s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
             |${tab}at Throws$$.main(Throws.scala:8)
             |${tab}at Throws.main(Throws.scala)
             |Caused by: java.lang.RuntimeException: nope
             |${tab}at scala.sys.package$$.error(package.scala:27)
             |${tab}at Throws$$.something(Throws.scala:3)
             |${tab}at Throws$$.main(Throws.scala:5)
             |$tab... 1 more
             |""".stripMargin.linesIterator.toVector
        else
          sys.error(s"Unexpected Scala version: $actualScalaVersion")
      if (exceptionLines != expectedLines) {
        pprint.log(exceptionLines)
        pprint.log(expectedLines)
      }
      assert(exceptionLines == expectedLines, clues(output))
    }
  }

  def stackTraceInScriptScala2(): Unit = {
    val inputs = TestInputs(
      Seq(
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
      Seq(
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

  val emptyInputs: TestInputs = TestInputs(Seq(os.rel / ".placeholder" -> ""))

  def piping(): Unit = {
    emptyInputs.fromRoot { root =>
      val cliCmd         = (TestUtil.cli ++ extraOptions).mkString(" ")
      val cmd            = s""" echo 'println("Hello" + " from pipe")' | $cliCmd _.sc """
      val res            = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from pipe" + System.lineSeparator()
      expect(res.out.text() == expectedOutput)
    }
  }

  if (!Properties.isWin) {
    test("piping") {
      piping()
    }
    test("Scripts accepted as piped input") {
      val message = "Hello"
      val input   = s"println(\"$message\")"
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "-", extraOptions)
          .call(cwd = root, stdin = input)
          .out.text().trim
        expect(output == message)
      }
    }
    test("Scala code accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput     = s"object Test extends App { println(\"$expectedOutput\") }"
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.scala", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.text().trim
        expect(output == expectedOutput)
      }
    }
    test("Scala code with references to existing files accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput =
        s"""object Test extends App {
           |  val data = SomeData(value = "$expectedOutput")
           |  println(data.value)
           |}""".stripMargin
      val inputs =
        TestInputs(Seq(os.rel / "SomeData.scala" -> "case class SomeData(value: String)"))
      inputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, ".", "_.scala", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.text().trim
        expect(output == expectedOutput)
      }
    }
    test("Java code accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput =
        s"""public class Main {
           |    public static void main(String[] args) {
           |        System.out.println("$expectedOutput");
           |    }
           |}
           |""".stripMargin
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.java", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.text().trim
        expect(output == expectedOutput)
      }
    }
    test("Java code with multiple classes accepted as piped input") {
      val expectedOutput = "Hello"
      val pipedInput =
        s"""class OtherClass {
           |    public String message;
           |    public OtherClass(String message) {
           |      this.message = message;
           |    }
           |}
           |
           |public class Main {
           |    public static void main(String[] args) {
           |        OtherClass obj = new OtherClass("$expectedOutput");
           |        System.out.println(obj.message);
           |    }
           |}
           |""".stripMargin
      emptyInputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, "_.java", extraOptions)
          .call(cwd = root, stdin = pipedInput)
          .out.text().trim
        expect(output == expectedOutput)
      }
    }
    test(
      "snippets mixed with piped Scala code and existing sources allow for cross-references"
    ) {
      val hello          = "Hello"
      val comma          = ", "
      val world          = "World"
      val exclamation    = "!"
      val expectedOutput = hello + comma + world + exclamation
      val scriptSnippet  = s"def world = \"$world\""
      val scalaSnippet   = "case class ScalaSnippetData(value: String)"
      val javaSnippet =
        s"public class JavaSnippet { public static String exclamation = \"$exclamation\"; }"
      val pipedInput = s"def hello = \"$hello\""
      val inputs =
        TestInputs(Seq(os.rel / "Main.scala" ->
          s"""object Main extends App {
             |  val hello = stdin.hello
             |  val comma = ScalaSnippetData(value = "$comma").value
             |  val world = snippet.world
             |  val exclamation = JavaSnippet.exclamation
             |  println(hello + comma + world + exclamation)
             |}
             |""".stripMargin))
      inputs.fromRoot { root =>
        val output =
          os.proc(
            TestUtil.cli,
            ".",
            "_.sc",
            "--script-snippet",
            scriptSnippet,
            "--scala-snippet",
            scalaSnippet,
            "--java-snippet",
            javaSnippet,
            extraOptions
          )
            .call(cwd = root, stdin = pipedInput)
            .out.text().trim
        expect(output == expectedOutput)
      }
    }
    test("pick .scala main class over in-context scripts, including piped ones") {
      val inputs = TestInputs(
        Seq(
          os.rel / "Hello.scala" ->
            """object Hello extends App {
              |  println(s"${stdin.hello} ${scripts.Script.world}")
              |}
              |""".stripMargin,
          os.rel / "scripts" / "Script.sc" -> """def world: String = "world""""
        )
      )
      val pipedInput = """def hello: String = "Hello""""
      inputs.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          ".",
          "_.sc"
        )
          .call(cwd = root, stdin = pipedInput)
        expect(res.out.text().trim == "Hello world")
      }
    }
    test("pick piped .scala main class over in-context scripts") {
      val inputs = TestInputs(
        Seq(
          os.rel / "Hello.scala" ->
            """object Hello {
              |  def hello: String = "Hello"
              |}
              |""".stripMargin,
          os.rel / "scripts" / "Script.sc" -> """def world: String = "world""""
        )
      )
      val pipedInput =
        """object Main extends App {
          |  println(s"${Hello.hello} ${scripts.Script.world}")
          |}
          |""".stripMargin
      inputs.fromRoot { root =>
        val res = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          ".",
          "_.scala"
        )
          .call(cwd = root, stdin = pipedInput)
        expect(res.out.text().trim == "Hello world")
      }
    }
  }

  def fd(): Unit = {
    emptyInputs.fromRoot { root =>
      val cliCmd         = (TestUtil.cli ++ extraOptions).mkString(" ")
      val cmd            = s""" $cliCmd <(echo 'println("Hello" + " from fd")') """
      val res            = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from fd" + System.lineSeparator()
      expect(res.out.text() == expectedOutput)
    }
  }

  if (!Properties.isWin)
    test("fd") {
      fd()
    }

  def escapedUrls(url: String): String =
    if (Properties.isWin) "\"" + url + "\""
    else url

  test("Script URL") {
    val url =
      "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/b0285fa0305f76856897517b06251970578565af/test.sc"
    val message = "Hello from GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Scala URL") {
    val url =
      "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala"
    val message = "Hello from Scala GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Java URL") {
    val url =
      "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.java"
    val message = "Hello from Java GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Github Gists Script URL") {
    val url =
      "https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec"
    val message = "Hello"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url))
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Zip with multiple Scala files") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""object Hello extends App {
             |  println(Messages.hello)
             |}
             |""".stripMargin,
        os.rel / "Messages.scala" ->
          s"""object Messages {
             |  def hello: String = "Hello"
             |}
             |""".stripMargin
      )
    )
    inputs.asZip { (root, zipPath) =>
      val message = "Hello"
      val output = os.proc(TestUtil.cli, extraOptions, zipPath.toString)
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Zip with Scala containing resource directive") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""//> using resourceDir "./"
             |import scala.io.Source
             |
             |object Hello extends App {
             |    val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
             |    println(inputs.mkString(","))
             |}
             |""".stripMargin,
        os.rel / "input" ->
          s"""1
             |2
             |""".stripMargin
      )
    )
    inputs.asZip { (root, zipPath) =>
      val message = "1,2"

      val output = os.proc(TestUtil.cli, extraOptions, zipPath.toString)
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  test("Zip with Scala Script containing resource directive") {
    val inputs = TestInputs(
      Seq(
        os.rel / "hello.sc" ->
          s"""//> using resourceDir "./"
             |import scala.io.Source
             |
             |val inputs = Source.fromResource("input").getLines.map(_.toInt).toSeq
             |println(inputs.mkString(","))
             |""".stripMargin,
        os.rel / "input" ->
          s"""1
             |2
             |""".stripMargin
      )
    )
    inputs.asZip { (root, zipPath) =>
      val message = "1,2"

      val output = os.proc(TestUtil.cli, extraOptions, zipPath.toString)
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
    }
  }

  def compileTimeOnlyJars(): Unit = {
    // format: off
    val cmd = Seq[os.Shellable](
      TestUtil.cs, "fetch",
      "--intransitive", "com.chuusai::shapeless:2.3.9",
      "--scala", actualScalaVersion
    )
    // format: on
    val shapelessJar = os.proc(cmd).call().out.text().trim
    expect(os.isFile(os.Path(shapelessJar, os.pwd)))

    val inputs = TestInputs(
      Seq(
        os.rel / "test.sc" ->
          """val shapelessFound =
            |  try Thread.currentThread().getContextClassLoader.loadClass("shapeless.HList") != null
            |  catch { case _: ClassNotFoundException => false }
            |println(if (shapelessFound) "Hello with " + "shapeless" else "Hello from " + "test")
            |""".stripMargin,
        os.rel / "Other.scala" ->
          """object Other {
            |  import shapeless._
            |  val l = 2 :: "a" :: HNil
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val baseOutput = os.proc(TestUtil.cli, extraOptions, ".", "--extra-jar", shapelessJar)
        .call(cwd = root)
        .out.text().trim
      expect(baseOutput == "Hello with shapeless")
      val output = os.proc(TestUtil.cli, extraOptions, ".", "--compile-only-jar", shapelessJar)
        .call(cwd = root)
        .out.text().trim
      expect(output == "Hello from test")
    }
  }

  // TODO Adapt this test to Scala 3
  if (actualScalaVersion.startsWith("2."))
    test("Compile-time only JARs") {
      compileTimeOnlyJars()
    }

  def commandLineScalacXOption(): Unit = {
    val inputs = TestInputs(
      Seq(
        os.rel / "Test.scala" ->
          """object Test {
            |  def main(args: Array[String]): Unit = {
            |    val msg = "Hello"
            |    val foo = List("Not printed", 2, true, new Object)
            |    println(msg)
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      def run(warnAny: Boolean) = {
        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, extraOptions, ".",
          if (warnAny) Seq("-Xlint:infer-any") else Nil
        )
        // format: on
        os.proc(cmd).call(
          cwd = root,
          stderr = os.Pipe
        )
      }

      val expectedWarning =
        "a type was inferred to be `Any`; this may indicate a programming error."

      val baseRes       = run(warnAny = false)
      val baseOutput    = baseRes.out.text().trim
      val baseErrOutput = baseRes.err.text()
      expect(baseOutput == "Hello")
      expect(!baseErrOutput.contains(expectedWarning))

      val res       = run(warnAny = true)
      val output    = res.out.text().trim
      val errOutput = res.err.text()
      expect(output == "Hello")
      expect(errOutput.contains(expectedWarning))
    }
  }

  if (actualScalaVersion.startsWith("2.12."))
    test("Command-line -X scalac options") {
      commandLineScalacXOption()
    }

  def commandLineScalacYOption(): Unit = {
    val inputs = TestInputs(
      Seq(
        os.rel / "Delambdafy.scala" ->
          """object Delambdafy {
            |  def main(args: Array[String]): Unit = {
            |    val l = List(0, 1, 2)
            |    println(l.map(_ + 1).mkString)
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      // FIXME We don't really use the run command here, in spite of being in RunTests…
      def classNames(inlineDelambdafy: Boolean): Seq[String] = {
        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "compile", extraOptions,
          "--class-path", ".",
          if (inlineDelambdafy) Seq("-Ydelambdafy:inline") else Nil
        )
        // format: on
        val res = os.proc(cmd).call(cwd = root)
        val cp  = res.out.text().trim.split(File.pathSeparator).toVector.map(os.Path(_, os.pwd))
        cp
          .filter(os.isDir(_))
          .flatMap(os.list(_))
          .filter(os.isFile(_))
          .map(_.last)
          .filter(_.startsWith("Delambdafy"))
          .filter(_.endsWith(".class"))
          .map(_.stripSuffix(".class"))
      }

      val baseClassNames = classNames(inlineDelambdafy = false)
      expect(baseClassNames.nonEmpty)
      expect(!baseClassNames.exists(_.contains("$anonfun$")))

      val classNames0 = classNames(inlineDelambdafy = true)
      expect(classNames0.exists(_.contains("$anonfun$")))
    }
  }

  if (actualScalaVersion.startsWith("2."))
    test("Command-line -Y scalac options") {
      commandLineScalacYOption()
    }

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("no JVM installed") {
      val fileName = "simple.sc"
      val message  = "Hello"
      val inputs = TestInputs(
        Seq(
          os.rel / fileName ->
            s"""val msg = "$message"
               |println(msg)
               |""".stripMargin
        )
      )
      inputs.fromRoot { root =>
        val baseImage =
          if (TestUtil.cliKind == "native-static")
            Constants.dockerAlpineTestImage
          else
            Constants.dockerTestImage
        os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
        val script =
          s"""#!/usr/bin/env sh
             |set -e
             |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName | tee -a output
             |""".stripMargin
        os.write(root / "script.sh", script)
        os.perms.set(root / "script.sh", "rwxr-xr-x")
        val termOpt = if (System.console() == null) Nil else Seq("-t")
        // format: off
        val cmd = Seq[os.Shellable](
          "docker", "run", "--rm", termOpt,
          "-v", s"$root:/data",
          "-w", "/data",
          ciOpt,
          baseImage,
          "/data/script.sh"
        )
        // format: on
        val res = os.proc(cmd).call(cwd = root)
        System.err.println(res.out.text())
        val output = os.read(root / "output").trim
        expect(output == message)
      }
    }

  test("Java options in config file") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""//> using javaOpt "-Dtest.message=$message"
             |val msg = sys.props("test.message")
             |println(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  test("Main class in config file") {
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.scala" ->
          s"""//> using `main-class` "hello"
             |object hello extends App { println("hello") }
             |object world extends App { println("world") }
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.text().trim
      expect(output == "hello")
    }
  }

  def simpleScriptDistrolessImage(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""val msg = "$message"
             |println(msg)
             |""".stripMargin,
        os.rel / "Dockerfile" ->
          os.read(os.Path(Constants.mostlyStaticDockerfile, os.pwd))
      )
    )
    inputs.fromRoot { root =>
      os.copy(os.Path(TestUtil.cli.head), root / "scala-cli")
      os.proc("docker", "build", "-t", "scala-cli-distroless-it", ".").call(
        cwd = root,
        stdout = os.Inherit
      )
      os.remove(root / "scala")
      os.remove(root / "Dockerfile")
      val termOpt   = if (System.console() == null) Nil else Seq("-t")
      val rawOutput = new ByteArrayOutputStream
      // format: off
      val cmd = Seq[os.Shellable](
        "docker", "run", "--rm", termOpt,
        "-v", s"$root:/data",
        "-w", "/data",
        ciOpt,
        "scala-cli-distroless-it",
        extraOptions,
        fileName
      )
      // format: on
      os.proc(cmd).call(
        cwd = root,
        stdout = os.ProcessOutput { (b, len) =>
          rawOutput.write(b, 0, len)
          System.err.write(b, 0, len)
        },
        mergeErrIntoOut = true
      )
      val output = new String(rawOutput.toByteArray, Charset.defaultCharset())
      expect(output.linesIterator.toVector.last == message)
    }
  }

  if (Properties.isLinux && TestUtil.cliKind == "native-mostly-static")
    test("simple distroless test") {
      simpleScriptDistrolessImage()
    }

  private def simpleDirInputs = TestInputs(
    Seq(
      os.rel / "dir" / "Hello.scala" ->
        """object Hello {
          |  def main(args: Array[String]): Unit = {
          |    val p = java.nio.file.Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
          |    println(p)
          |  }
          |}
          |""".stripMargin
    )
  )
  private def nonWritableTest(): Unit = {
    simpleDirInputs.fromRoot { root =>
      def run(): String = {
        val res = os.proc(TestUtil.cli, "dir").call(cwd = root)
        res.out.text().trim
      }

      val classDirBefore = os.Path(run(), os.pwd)
      expect(classDirBefore.startsWith(root))

      try {
        os.perms.set(root / "dir", "r-xr-xr-x")
        val classDirAfter = os.Path(run(), os.pwd)
        expect(!classDirAfter.startsWith(root))
      }
      finally os.perms.set(root / "dir", "rwxr-xr-x")
    }
  }
  if (!Properties.isWin)
    test("no .scala in non-writable directory") {
      nonWritableTest()
    }

  private def forbiddenDirTest(): Unit = {
    simpleDirInputs.fromRoot { root =>
      def run(options: String*): String = {
        val res = os.proc(TestUtil.cli, "dir", options).call(cwd = root)
        res.out.text().trim
      }

      val classDirBefore = os.Path(run(), os.pwd)
      expect(classDirBefore.startsWith(root))

      val classDirAfter = os.Path(run("--forbid", "./dir"), os.pwd)
      expect(!classDirAfter.startsWith(root))
    }
  }
  if (!Properties.isWin)
    test("no .scala in forbidden directory") {
      forbiddenDirTest()
    }

  private def resourcesInputs(directive: String = "") = {
    val resourceContent = "Hello from resources"
    TestInputs(
      Seq(
        os.rel / "src" / "proj" / "resources" / "test" / "data" -> resourceContent,
        os.rel / "src" / "proj" / "Test.scala" ->
          s"""$directive
             |object Test {
             |  def main(args: Array[String]): Unit = {
             |    val cl = Thread.currentThread().getContextClassLoader
             |    val is = cl.getResourceAsStream("test/data")
             |    val content = scala.io.Source.fromInputStream(is)(scala.io.Codec.UTF8).mkString
             |    assert(content == "$resourceContent")
             |  }
             |}
             |""".stripMargin
      )
    )
  }
  test("resources") {
    resourcesInputs().fromRoot { root =>
      os.proc(TestUtil.cli, "run", "src", "--resource-dirs", "./src/proj/resources").call(cwd =
        root
      )
    }
  }
  test("resources via directive") {
    resourcesInputs("//> using resourceDirs \"./resources\"").fromRoot { root =>
      os.proc(TestUtil.cli, "run", ".").call(cwd = root)
    }
  }

  def argsAsIsTest(): Unit = {
    val inputs = TestInputs(
      Seq(
        os.rel / "MyScript.scala" ->
          """#!/usr/bin/env -S scala-cli shebang
            |object MyScript {
            |  def main(args: Array[String]): Unit =
            |    println("Hello" + args.map(" " + _).mkString)
            |}
            |""".stripMargin
      )
    )
    val launcherPath = TestUtil.cli match {
      case Seq(cli) => os.Path(cli, os.pwd)
      case other => sys.error(s"Expected CLI command to be just a path to a launcher (got $other)")
    }
    inputs.fromRoot { root =>
      os.perms.set(root / "MyScript.scala", "rwxrwxr-x")
      val binDir = root / "bin"
      os.makeDir.all(binDir)
      os.copy(launcherPath, binDir / "scala-cli")
      val updatedPath =
        binDir.toString + File.pathSeparator + Option(System.getenv("PATH")).getOrElse("")
      val res = os.proc("/bin/bash", "-c", "./MyScript.scala from tests")
        .call(cwd = root, env = Map("PATH" -> updatedPath))
      expect(res.out.text().trim == "Hello from tests")
    }
  }
  if (TestUtil.isNativeCli && !Properties.isWin)
    test("should pass arguments as is") {
      argsAsIsTest()
    }

  test("test scope") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Main.scala" ->
          """//> using lib "com.lihaoyi::utest:0.7.10"
            |
            |object Main {
            |  val err = utest.compileError("pprint.log(2)")
            |  def message = "Hello from " + "tests"
            |  def main(args: Array[String]): Unit = {
            |    println(message)
            |    println(err)
            |  }
            |}
            |""".stripMargin,
        os.rel / "Tests.scala" ->
          """//> using lib "com.lihaoyi::pprint:0.6.6"
            |//> using target.scope "test"
            |
            |import utest._
            |
            |object Tests extends TestSuite {
            |  val tests = Tests {
            |    test("message") {
            |      assert(Main.message.startsWith("Hello"))
            |    }
            |  }
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root)
      pprint.log(res.out.text())
      expect(res.out.text().contains("Hello from tests"))
    }
  }
  test("interconnection between scripts") {
    val inputs = TestInputs(
      Seq(
        os.rel / "f.sc"     -> "def f(x: String) = println(x + x + x)",
        os.rel / "main0.sc" -> "f.f(args(0))"
      )
    )
    inputs.fromRoot { root =>
      val p =
        os.proc(TestUtil.cli, "main0.sc", "f.sc", "--", "20").call(cwd = root)
      val res = p.out.text().trim
      expect(res == "202020")
    }
  }
  test("CLI args passed to script") {
    val inputs = TestInputs(
      Seq(
        os.rel / "f.sc" -> "println(args(0))"
      )
    )
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "f.sc", "--", "16").call(cwd = root)
      expect(p.out.text().trim == "16")
    }
  }

  if (!Properties.isWin) {
    test("CLI args passed to shebang script") {
      val inputs = TestInputs(
        Seq(
          os.rel / "f.sc" -> s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang -S 2.13
                                 |//> using scala "$actualScalaVersion"
                                 |println(args.toList)""".stripMargin
        )
      )
      inputs.fromRoot { root =>
        os.perms.set(root / "f.sc", os.PermSet.fromString("rwx------"))
        val p = os.proc("./f.sc", "1", "2", "3", "-v").call(cwd = root)
        expect(p.out.text().trim == "List(1, 2, 3, -v)")
      }
    }
    test("CLI args passed to shebang in Scala file") {
      val inputs = TestInputs(
        Seq(
          os.rel / "f.scala" -> s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang
                                    |object Hello {
                                    |    def main(args: Array[String]) = {
                                    |        println(args.toList)
                                    |    }
                                    |}
                                    |""".stripMargin
        )
      )
      inputs.fromRoot { root =>
        os.perms.set(root / "f.scala", os.PermSet.fromString("rwx------"))
        val p = os.proc("./f.scala", "1", "2", "3", "-v").call(cwd = root)
        expect(p.out.text().trim == "List(1, 2, 3, -v)")
      }
    }
  }

  test("Runs with JVM 8") {
    val inputs = TestInputs(
      Seq(
        os.rel / "run.scala" -> """object Main extends App { println("hello")}"""
      )
    )
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "run.scala", "--jvm", "8").call(cwd = root)
      expect(p.out.text().trim == "hello")
    }
  }

  test("workspace dir") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """|//> using lib "com.lihaoyi::os-lib:0.7.8"
             |
             |object Hello extends App {
             |  println(os.pwd)
             |}""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "Hello.scala").call(cwd = root)
      expect(p.out.text().trim == root.toString)
    }
  }

  test("help command") {
    for { helpOption <- Seq("help", "-help", "--help") } {
      val help = os.proc(TestUtil.cli, helpOption).call(check = false)
      assert(help.exitCode == 0, clues(helpOption, help.out.text(), help.err.text(), help.exitCode))
      expect(help.out.text().contains("Usage:"))
    }
  }

  test("help js and native") {
    val helpJsOption     = "--help-js"
    val helpNativeOption = "--help-native"
    val helpJs           = os.proc(TestUtil.cli, helpJsOption).call(check = false)
    val helpNative       = os.proc(TestUtil.cli, helpNativeOption).call(check = false)

    expect(helpJs.out.text().contains("Scala.js options"))
    expect(!helpJs.out.text().contains("Scala Native options"))

    expect(helpNative.out.text().contains("Scala Native options"))
    expect(!helpNative.out.text().contains("Scala.js options"))
  }

  test("version command") {
    // tests if the format is correct instead of comparing to a version passed via Constants
    // in order to catch errors in Mill configuration, too
    val versionRegex = ".*\\d+[.]\\d+[.]\\d+.*".r
    for (versionOption <- Seq("version", "--version")) {
      val version = os.proc(TestUtil.cli, versionOption).call(check = false)
      assert(
        versionRegex.findFirstMatchIn(version.out.text()).isDefined,
        clues(version.exitCode, version.out.text(), version.err.text())
      )
      expect(version.exitCode == 0)
    }
  }

  test("-D.. options passed to the child app") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" -> """object ClassHello extends App {
                                    |  print(System.getProperty("foo"))
                                    |}""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "Hello.scala", "--java-opt", "-Dfoo=bar").call(
        cwd = root
      )
      expect(res.out.text().trim() == "bar")
    }
  }

  test("-X.. options passed to the child app") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" -> "object Hello extends App {}"
      )
    )
    inputs.fromRoot { root =>
      // Binaries generated with Graal's native-image are run under SubstrateVM
      // that cuts some -X.. java options, so they're not passed
      // to the application's main method. This test ensures it is not
      // cut. "--java-opt" option requires a value, so it would fail
      // if -Xmx1g is cut
      val res = os.proc(TestUtil.cli, "Hello.scala", "--java-opt", "-Xmx1g").call(
        cwd = root,
        check = false
      )
      assert(res.exitCode == 0, clues(res.out.text(), res.err.text()))
    }
  }

  def sudoTest(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""val msg = "$message"
             |println(msg)
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val baseImage = Constants.dockerTestImage
      os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
      val script =
        s"""#!/usr/bin/env sh
           |set -ev
           |useradd --create-home --shell /bin/bash test
           |apt update
           |apt install -y sudo
           |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName | tee output-root
           |sudo -u test ./scala clean $fileName
           |sudo -u test ./scala ${extraOptions.mkString(
            " "
          ) /* meh escaping */} $fileName | tee output-user
           |""".stripMargin
      os.write(root / "script.sh", script)
      os.perms.set(root / "script.sh", "rwxr-xr-x")
      val termOpt = if (System.console() == null) Nil else Seq("-t")
      // format: off
      val cmd = Seq[os.Shellable](
        "docker", "run", "--rm", termOpt,
        "-v", s"$root:/data",
        "-w", "/data",
        ciOpt,
        baseImage,
        "/data/script.sh"
      )
      // format: on
      os.proc(cmd).call(cwd = root, stdout = os.Inherit)
      val rootOutput = os.read(root / "output-root").trim
      expect(rootOutput == message)
      val userOutput = os.read(root / "output-user").trim
      expect(userOutput == message)
    }
  }

  if (Properties.isLinux && TestUtil.isNativeCli && TestUtil.cliKind != "native-static")
    test("sudo") {
      sudoTest()
    }

  def authProxyTest(): Unit = {
    val okDir    = os.rel / "ok"
    val wrongDir = os.rel / "wrong"
    val inputs = TestInputs(
      Seq(okDir, wrongDir).flatMap { baseDir =>
        Seq(
          baseDir / "Simple.scala" ->
            """object Simple {
              |  def main(args: Array[String]): Unit = {
              |    println("Hello proxy")
              |  }
              |}
              |""".stripMargin
        )
      }
    )
    def authProperties(host: String, port: Int, user: String, password: String): Seq[String] =
      Seq("http", "https").flatMap { scheme =>
        Seq(
          s"-D$scheme.proxyHost=$host",
          s"-D$scheme.proxyPort=$port",
          s"-D$scheme.proxyUser=$user",
          s"-D$scheme.proxyPassword=$password",
          s"-D$scheme.proxyProtocol=http"
        )
      }
    val proxyArgs      = authProperties("localhost", 9083, "jack", "insecure")
    val wrongProxyArgs = authProperties("localhost", 9084, "wrong", "nope")
    val image          = Constants.authProxyTestImage
    inputs.fromRoot { root =>
      DockerServer.withServer(image, root.toString, 80 -> 9083) { _ =>
        DockerServer.withServer(image, root.toString, 80 -> 9084) { _ =>

          val okRes = os.proc(
            TestUtil.cli,
            proxyArgs,
            "-Dcoursier.cache.throw-exceptions=true",
            "run",
            ".",
            "--cache",
            os.rel / "tmp-cache-ok"
          )
            .call(cwd = root / okDir)
          val okOutput = okRes.out.text().trim
          expect(okOutput == "Hello proxy")

          val wrongRes = os.proc(
            TestUtil.cli,
            wrongProxyArgs,
            "-Dcoursier.cache.throw-exceptions=true",
            "run",
            ".",
            "--cache",
            os.rel / "tmp-cache-wrong"
          )
            .call(cwd = root / wrongDir, mergeErrIntoOut = true, check = false)
          val wrongOutput = wrongRes.out.text().trim
          expect(wrongRes.exitCode == 1)
          expect(wrongOutput.contains(
            """Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required""""
          ))
        }
      }
    }
  }

  def runAuthProxyTest: Boolean =
    Properties.isLinux || (Properties.isMac && !TestUtil.isCI)
  if (runAuthProxyTest)
    test("auth proxy") {
      authProxyTest()
    }

  def jsDomTest(): Unit = {
    val inputs = TestInputs(
      Seq(
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

  test("UTF-8") {
    val message  = "Hello from TestÅÄÖåäö"
    val fileName = "TestÅÄÖåäö.scala"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""object TestÅÄÖåäö {
             |  def main(args: Array[String]): Unit = {
             |    println("$message")
             |  }
             |}
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "-Dtest.scala-cli.debug-charset-issue=true",
        "run",
        extraOptions,
        fileName
      )
        .call(cwd = root)
      if (res.out.text(Codec.default).trim != message) {
        pprint.err.log(res.out.text(Codec.default).trim)
        pprint.err.log(message)
      }
      expect(res.out.text(Codec.default).trim == message)
    }
  }

  test("scalac help") {
    emptyInputs.fromRoot { root =>
      val res1 = os.proc(
        TestUtil.cli,
        extraOptions,
        "--scalac-help"
      )
        .call(cwd = root, mergeErrIntoOut = true)
      expect(res1.out.text().contains("scalac <options> <source files>"))

      val res2 = os.proc(
        TestUtil.cli,
        extraOptions,
        "--scalac-option",
        "-help"
      )
        .call(cwd = root, mergeErrIntoOut = true)
      expect(res1.out.text() == res2.out.text())
    }
  }

  test("scalac print options") {
    emptyInputs.fromRoot { root =>
      val printOptionsForAllVersions = Seq("-X", "-Xshow-phases", "-Y")
      val printOptionsSince213       = Seq("-V", "-Vphases", "-W")
      val version213OrHigher =
        actualScalaVersion.startsWith("2.13") || actualScalaVersion.startsWith("3")
      val printOptionsToTest = printOptionsForAllVersions ++
        (
          if (version213OrHigher) printOptionsSince213
          else Seq.empty
        )
      printOptionsToTest.foreach { printOption =>
        val res = os.proc(
          TestUtil.cli,
          extraOptions,
          printOption
        )
          .call(cwd = root, mergeErrIntoOut = true)
        expect(res.out.text().nonEmpty)
      }
    }
  }

  test("pick .scala main class over in-context scripts") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          """object Hello extends App {
            |  println(s"Hello ${scripts.Script.world}")
            |}
            |""".stripMargin,
        os.rel / "scripts" / "Script.sc" -> """def world: String = "world"""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        "."
      )
        .call(cwd = root)
      expect(res.out.text().trim == "Hello world")
    }
  }

  test("return relevant error if multiple .scala main classes are present") {
    val (scalaFile1, scalaFile2, scriptName) = ("ScalaMainClass1", "ScalaMainClass2", "ScalaScript")
    val scriptsDir                           = "scritps"
    val inputs = TestInputs(
      Seq(
        os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
        os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
        os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        "."
      )
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode == 1)
      val output          = res.out.text().trim
      val Some(errorLine) = output.linesIterator.find(_.contains("Found several main classes"))
      val mainClasses     = errorLine.split(":").last.trim.split(", ").toSet
      expect(mainClasses == Set(scalaFile1, scalaFile2, s"$scriptsDir.${scriptName}_sc"))
    }
  }

  test(
    "return relevant error when main classes list is requested, but no main classes are present"
  ) {
    val inputs = TestInputs(Seq(os.rel / "Main.scala" -> "object Main { println() }"))
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        ".",
        "--main-class-ls"
      )
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode == 1)
      expect(res.out.text().trim.contains("No main class found"))
    }
  }

  test("correctly list main classes") {
    val (scalaFile1, scalaFile2, scriptName) = ("ScalaMainClass1", "ScalaMainClass2", "ScalaScript")
    val scriptsDir                           = "scripts"
    val inputs = TestInputs(
      Seq(
        os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
        os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
        os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        ".",
        "--list-main-classes"
      )
        .call(cwd = root)
      val output      = res.out.text().trim
      val mainClasses = output.split(" ").toSet
      expect(mainClasses == Set(scalaFile1, scalaFile2, s"$scriptsDir.${scriptName}_sc"))
    }
  }

  test("correctly run a script snippet") {
    emptyInputs.fromRoot { root =>
      val msg =
        "123456" // FIXME: change this to a a non-numeric string when Windows encoding is handled properly
      val res = os.proc(TestUtil.cli, "-e", s"println($msg)", extraOptions).call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }

  test("correctly run a scala snippet") {
    emptyInputs.fromRoot { root =>
      val msg =
        "123456" // FIXME: change this to a a non-numeric string when Windows encoding is handled properly
      val res =
        os.proc(
          TestUtil.cli,
          "--scala-snippet",
          s"object Hello extends App { println($msg) }",
          extraOptions
        )
          .call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }

  test("correctly run a java snippet") {
    emptyInputs.fromRoot { root =>
      val msg =
        "123456" // FIXME: change this to a a non-numeric string when Windows encoding is handled properly
      val res = os.proc(
        TestUtil.cli,
        "--java-snippet",
        s"public class Main { public static void main(String[] args) { System.out.println($msg); } }",
        extraOptions
      )
        .call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }
}
