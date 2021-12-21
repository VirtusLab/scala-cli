package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.Charset

import scala.util.Properties

abstract class RunTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  def simpleScriptTest(ignoreErrors: Boolean = false): Unit = {
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
      val output = os.proc(TestUtil.cli, extraOptions, fileName).call(cwd = root).out.text().trim
      if (!ignoreErrors)
        expect(output == message)
    }
  }

  // warm-up run that downloads compiler bridges
  // The "Downloading compiler-bridge (from bloop?) pollute the output, and would make the first test fail.
  lazy val warmupTest = {
    System.err.println("Running RunTests warmup test…")
    simpleScriptTest(ignoreErrors = true)
    System.err.println("Done running RunTests warmup test.")
  }

  override def test(name: String)(body: => Any)(implicit loc: munit.Location): Unit =
    super.test(name) { warmupTest; body }(loc)

  test("simple script") {
    simpleScriptTest()
  }

  def simpleJsTest(): Unit = {
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
      val output =
        os.proc(TestUtil.cli, extraOptions, fileName, "--js").call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunJs)
    test("simple script JS") {
      simpleJsTest()
    }

  def simpleJsViaConfigFileTest(): Unit = {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""// using platform "scala-js"
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

  if (TestUtil.canRunJs)
    test("simple script JS via config file") {
      simpleJsViaConfigFileTest()
    }

  def platformNl = if (Properties.isWin) "\\r\\n" else "\\n"

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
        os.proc(TestUtil.cli, extraOptions, fileName, "--native").call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2.13"))
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
      inputs.fromRoot { root =>
        val output = os.proc(TestUtil.cli, extraOptions, "--native", "a.sc").call(
          cwd = root,
          check = false,
          stderr = os.Pipe
        ).err.text().trim
        expect(output.contains("scala-cli: invalid option:"))
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
        root).out.text().trim
      expect(output == message)
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--js").call(cwd =
        root).out.text().trim
      expect(output == message)
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
      val output =
        os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--native").call(cwd =
          root).out.text().trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2.13"))
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
        root).out.text().trim
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--js", "--main-class", "print_sc")
        .call(cwd = root)
        .out.text().trim
      expect(output == message)
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
      val output =
        os.proc(TestUtil.cli, extraOptions, "dir", "--native", "--main-class", "print_sc")
          .call(cwd = root)
          .out.text().trim
      expect(output == message)
    }
  }

  // TODO: make nice messages that the scenario is unsupported with 2.12
  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2.13"))
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
      val res = os.proc(cmd).call(cwd = root, check = false, mergeErrIntoOut = true)
      // FIXME We need to have the pretty-stacktraces stuff take scala.colored-stack-traces into account
      val exceptionLines =
        res.out.lines().map(stripAnsi).dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val sp  = " "
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
        else if (actualScalaVersion.startsWith("2.13."))
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
          s"""Exception in thread main: java.lang.Exception: Caught exception during processing
             |    at method main in Throws.scala:8$sp
             |
             |Caused by: Exception in thread main: java.lang.RuntimeException: nope
             |    at method error in scala.sys.package$$:27$sp
             |    at method something in Throws.scala:3$sp
             |    at method main in Throws.scala:5$sp
             |
             |""".stripMargin.linesIterator.toVector
      if (exceptionLines != expectedLines) {
        pprint.log(exceptionLines)
        pprint.log(expectedLines)
      }
      expect(exceptionLines == expectedLines)
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
      val exceptionLines = res.out.lines().dropWhile(!_.startsWith("Exception in thread "))
      val tab            = "\t"
      val expectedLines =
        if (actualScalaVersion.startsWith("2.12."))
          s"""Exception in thread "main" java.lang.ExceptionInInitializerError
             |${tab}at throws_sc$$.main(throws.sc:23)
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
             |${tab}at throws_sc$$.main(throws.sc:23)
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
      expect(exceptionLines.length == expectedLines.length)
      for { i <- 0 until exceptionLines.length } expect(exceptionLines(i) == expectedLines(i))
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
        TestUtil.cli, "run", extraOptions, ".",
        "--java-prop=scala.cli.runner.Stacktrace.disable=true"
      )
      // format: on
      val res = os.proc(cmd).call(cwd = root, check = false, mergeErrIntoOut = true)
      val exceptionLines = res.out.lines()
        .map(stripAnsi)
        .dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
        s"""Exception in thread "main" java.lang.ExceptionInInitializerError
           |${tab}at throws_sc$$.main(throws.sc:25)
           |${tab}at throws_sc.main(throws.sc)
           |Caused by: java.lang.Exception: Caught exception during processing
           |${tab}at throws$$.<clinit>(throws.sc:8)
           |$tab... 2 more
           |Caused by: java.lang.RuntimeException: nope
           |${tab}at scala.sys.package$$.error(package.scala:27)
           |${tab}at throws$$.something(throws.sc:3)
           |${tab}at throws$$.<clinit>(throws.sc:5)
           |$tab... 2 more""".stripMargin.linesIterator.toVector
      expect(exceptionLines.length == expectedLines.length)
      for { i <- 0 until exceptionLines.length } expect(exceptionLines(i) == expectedLines(i))
    }
  }

  if (actualScalaVersion.startsWith("3."))
    test("stack traces in script in Scala 3") {
      scriptStackTraceScala3()
    }

  val emptyInputs = TestInputs(
    Seq(
      os.rel / ".placeholder" -> ""
    )
  )

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

  test("Zip with Scala containing resource directive") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""// using resourceDir "./"
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
          s"""// using resourceDir "./"
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
      "--intransitive", "com.chuusai::shapeless:2.3.7",
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
          "-v", s"${root}:/data",
          "-w", "/data",
          baseImage,
          "/data/script.sh"
        )
        // format: on
        os.proc(cmd).call(cwd = root)
        val output = os.read(root / "output").trim
        expect(output == message)
      }
    }

  test("Java options in config file") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          s"""// using javaOpt "-Dtest.message=$message"
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
          s"""// using `main-class` "hello"
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
          """FROM gcr.io/distroless/base-debian10
            |ADD scala /usr/local/bin/scala
            |ENTRYPOINT ["/usr/local/bin/scala"]
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.copy(os.Path(TestUtil.cli.head), root / "scala")
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
        root)
    }
  }
  test("resources via directive") {
    resourcesInputs("// using resourceDirs \"./resources\"").fromRoot { root =>
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
          """// using lib "com.lihaoyi::utest:0.7.10"
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
          """// using lib "com.lihaoyi::pprint:0.6.6"
            |// using target.scope "test"
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
                                 |// using scala "$actualScalaVersion"
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
          """|// using lib "com.lihaoyi::os-lib:0.7.8"
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
}
