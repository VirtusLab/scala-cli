package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class RunTests extends munit.FunSuite {

  def simpleScriptTest(ignoreErrors: Boolean = false): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
         s"""val msg = "$message"
            |println(msg)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, fileName).call(cwd = root).out.text.trim
      if (!ignoreErrors)
        expect(output == message)
    }
  }

  // warm-up run that downloads compiler bridges
  // The "Downloading compiler-bridge (from bloop?) pollute the output, and would make the first test fail.
  simpleScriptTest(ignoreErrors = true)

  test("simple script") {
    simpleScriptTest()
  }

  def simpleJsTest(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
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
      val output = os.proc(TestUtil.cli, fileName, "--js").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunJs)
    test("simple script JS") {
      simpleJsTest()
    }

  def platformNl = if (Properties.isWin) "\\r\\n" else "\\n"

  def simpleNativeTests(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
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
      val output = os.proc(TestUtil.cli, fileName, "--native").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative)
    test("simple script native") {
      simpleNativeTests()
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc", "--js").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, "print.sc", "messages.sc", "--native").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative)
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
      val output = os.proc(TestUtil.cli, "dir", "--main-class", "print").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Current directory as default") {
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
      val output = os.proc(TestUtil.cli, "run", "--main-class", "print").call(cwd = root / "dir").out.text.trim
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
      val res = os.proc(TestUtil.cli, "--main-class", "print")
        .call(cwd = root / "dir", check = false, mergeErrIntoOut = true)
      val output = res.out.text.trim
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
      val output = os.proc(TestUtil.cli, "run", "--", message).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Pass arguments - Scala 3") {
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
      val output = os.proc(TestUtil.cli, "run", "--scala", "3.0.0", "--", message).call(cwd = root).out.text.trim
      expect(output == message)
    }
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
      val output = os.proc(TestUtil.cli, "dir", "--js", "--main-class", "print").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, "dir", "--native", "--main-class", "print").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative)
    test("Directory native") {
      directoryNative()
    }

  test("sub-directory") {
    val fileName = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
    val inputs = TestInputs(
      Seq(
        scriptPath ->
         s"""println(getClass.getName)
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, scriptPath.toString)
        .call(cwd = root)
        .out.text
        .trim
      expect(output == expectedClassName)
    }
  }

  test("sub-directory and script") {
    val fileName = "script.sc"
    val expectedClassName = fileName.stripSuffix(".sc") + "$"
    val scriptPath = os.rel / "something" / fileName
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
      val output = os.proc(TestUtil.cli, "dir", scriptPath.toString)
        .call(cwd = root)
        .out.text
        .trim
      expect(output == expectedClassName)
    }
  }

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
      val res = os.proc(TestUtil.cli, "run", "--java-prop", "scala.colored-stack-traces=false")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      val exceptionLines = res.out.lines.dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
       s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
          |${tab}at Throws$$.main(Throws.scala:8)
          |${tab}at Throws.main(Throws.scala)
          |Caused by: java.lang.RuntimeException: nope
          |${tab}at scala.sys.package$$.error(package.scala:30)
          |${tab}at Throws$$.something(Throws.scala:3)
          |${tab}at Throws$$.main(Throws.scala:5)
          |${tab}... 1 more
          |""".stripMargin.linesIterator.toVector
      expect(exceptionLines == expectedLines)
    }
  }

  test("stack traces in script") {
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
      val res = os.proc(TestUtil.cli, "run", "--java-prop", "scala.colored-stack-traces=false")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      val exceptionLines = res.out.lines.dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
       s"""Exception in thread "main" java.lang.ExceptionInInitializerError
          |${tab}at throws.main(throws.sc)
          |Caused by: java.lang.Exception: Caught exception during processing
          |${tab}at throws$$.<init>(throws.sc:6)
          |${tab}at throws$$.<clinit>(throws.sc)
          |${tab}... 1 more
          |Caused by: java.lang.RuntimeException: nope
          |${tab}at scala.sys.package$$.error(package.scala:30)
          |${tab}at throws$$.something(throws.sc:2)
          |${tab}at throws$$.<init>(throws.sc:3)
          |${tab}... 2 more
          |""".stripMargin.linesIterator.toVector
      expect(exceptionLines == expectedLines)
    }
  }

  test("stack traces in script in Scala 3") {
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
      val res = os.proc(TestUtil.cli, "run", "--scala", "3.0.0", "--runner=false")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      val exceptionLines = res.out.lines.dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
       s"""Exception in thread "main" java.lang.ExceptionInInitializerError
          |${tab}at throws.main(throws.sc)
          |Caused by: java.lang.Exception: Caught exception during processing
          |${tab}at throws$$.<clinit>(throws.sc:8)
          |${tab}... 1 more
          |Caused by: java.lang.RuntimeException: nope
          |${tab}at scala.sys.package$$.error(package.scala:27)
          |${tab}at throws$$.something(throws.sc:3)
          |${tab}at throws$$.<clinit>(throws.sc:5)
          |${tab}... 1 more
          |""".stripMargin.linesIterator.toVector
      expect(exceptionLines == expectedLines)
    }
  }

  def piping(): Unit = {
    TestInputs(Nil).fromRoot { root =>
      val cmd = s""" echo 'println("Hello" + " from pipe")' | ${TestUtil.cli.mkString(" ")} _.sc """
      val res = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from pipe" + System.lineSeparator()
      expect(res.out.text == expectedOutput)
    }
  }

  if (!Properties.isWin)
    test("piping") {
      piping()
    }

  def fd(): Unit = {
    TestInputs(Nil).fromRoot { root =>
      val cmd = s""" ${TestUtil.cli.mkString(" ")} <(echo 'println("Hello" + " from fd")') """
      val res = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from fd" + System.lineSeparator()
      expect(res.out.text == expectedOutput)
    }
  }

  if (!Properties.isWin)
    test("fd") {
      fd()
    }

  val printScalaVersionInputs = TestInputs(
    Seq(
      os.rel / "print.sc" ->
       s"""println(scala.util.Properties.versionNumberString)
          |""".stripMargin
    )
  )
  val printScalaVersionInputs3 = TestInputs(
    Seq(
      os.rel / "print.sc" ->
       s"""def printStuff(): Unit =
          |  val toPrint = scala.util.Properties.versionNumberString
          |  println(toPrint)
          |printStuff()
          |""".stripMargin
    )
  )
  test("Scala version 2.12") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, ".", "--scala", "2.12").call(cwd = root).out.text.trim
      assert(output.startsWith("2.12."))
    }
  }
  test("Scala version 2.13") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, ".", "--scala", "2.13").call(cwd = root).out.text.trim
      assert(output.startsWith("2.13."))
    }
  }
  test("Scala version 2") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, ".", "--scala", "2").call(cwd = root).out.text.trim
      assert(output.startsWith("2.13."))
    }
  }
  test("Scala version 3") {
    printScalaVersionInputs3.fromRoot { root =>
      val output = os.proc(TestUtil.cli, ".", "--scala", "3").call(cwd = root).out.text.trim
      // Scala 3.0 uses the 2.13 standard library
      assert(output.startsWith("2.13."))
    }
  }

}
