package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, fileName).call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, fileName, "--js").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, fileName, "--native").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "print.sc", "messages.sc").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "print.sc", "messages.sc", "--js").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "print.sc", "messages.sc", "--native").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "dir", "--main-class", "print").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--main-class", "print").call(cwd = root / "dir").out.text.trim
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
      val res = os.proc(TestUtil.cli, TestUtil.extraOptions, "--main-class", "print")
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
      val output = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--", message).call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--scala", "3.0.0", "--", message).call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "dir", "--js", "--main-class", "print").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "dir", "--native", "--main-class", "print").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, scriptPath.toString)
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, "dir", scriptPath.toString)
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
      val res = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--java-prop", "scala.colored-stack-traces=false")
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
      val res = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--java-prop", "scala.colored-stack-traces=false")
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
      val res = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, "--scala", "3.0.0", "--runner=false")
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

  val emptyInputs = TestInputs(
    Seq(
      os.rel / ".placeholder" -> ""
    )
  )

  def piping(): Unit = {
    emptyInputs.fromRoot { root =>
      val cmd = s""" echo 'println("Hello" + " from pipe")' | ${TestUtil.cli.mkString(" ")} ${TestUtil.extraOptions.mkString(" ")} _.sc """
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
    emptyInputs.fromRoot { root =>
      val cmd = s""" ${TestUtil.cli.mkString(" ")} ${TestUtil.extraOptions.mkString(" ")} <(echo 'println("Hello" + " from fd")') """
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
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2.12").call(cwd = root).out.text.trim
      assert(output.startsWith("2.12."))
    }
  }
  test("Scala version 2.13") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2.13").call(cwd = root).out.text.trim
      assert(output.startsWith("2.13."))
    }
  }
  test("Scala version 2") {
    printScalaVersionInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "2").call(cwd = root).out.text.trim
      assert(output.startsWith("2.13."))
    }
  }
  test("Scala version 3") {
    printScalaVersionInputs3.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--scala", "3").call(cwd = root).out.text.trim
      // Scala 3.0 uses the 2.13 standard library
      assert(output.startsWith("2.13."))
    }
  }

  def escapedUrls(url: String): String =
    if (Properties.isWin) "\"" + url + "\""
    else url

  test("Script URL") {
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/b0285fa0305f76856897517b06251970578565af/test.sc"
    val message = "Hello from GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Scala URL") {
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala"
    val message = "Hello from Scala GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Java URL") {
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.java"
    val message = "Hello from Java GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Compile-time only JARs") {
    val shapelessJar = os.proc("cs", "fetch", "--intransitive", "com.chuusai:shapeless_2.12:2.3.7").call().out.text.trim
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
      val baseOutput = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--extra-jar", shapelessJar).call(cwd = root).out.text.trim
      expect(baseOutput == "Hello with shapeless")
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".", "--compile-only-jar", shapelessJar).call(cwd = root).out.text.trim
      expect(output == "Hello from test")
    }
  }

  test("Scala version in config file") {
    val inputs = TestInputs(
      Seq(
        os.rel / "test.sc" ->
          """println(scala.util.Properties.versionNumberString)
            |""".stripMargin,
        os.rel / "scala.conf" ->
          """scala {
            |  version = 2.13.1
            |}
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, TestUtil.extraOptions, ".").call(cwd = root).out.text.trim
      expect(output == "2.13.1")
    }
  }

  test("Command-line -X scalac options") {
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
      def run(warnAny: Boolean) =
        os.proc(TestUtil.cli, TestUtil.extraOptions, ".", if (warnAny) Seq("-Xlint:infer-any") else Nil).call(
          cwd = root,
          stderr = os.Pipe
        )

      val expectedWarning = "a type was inferred to be `Any`; this may indicate a programming error."

      val baseRes = run(warnAny = false)
      val baseOutput = baseRes.out.text.trim
      val baseErrOutput = baseRes.err.text
      expect(baseOutput == "Hello")
      expect(!baseErrOutput.contains(expectedWarning))

      val res = run(warnAny = true)
      val output = res.out.text.trim
      val errOutput = res.err.text
      expect(output == "Hello")
      expect(errOutput.contains(expectedWarning))
    }
  }

  test("Command-line -Y scalac options") {
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
        val res = os.proc(TestUtil.cli, "compile", TestUtil.extraOptions, "--class-path", ".", if (inlineDelambdafy) Seq("-Ydelambdafy:inline") else Nil)
          .call(cwd = root)
        val cp = res.out.text.trim.split(File.pathSeparator).map(os.Path(_, os.pwd))
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

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("no JVM installed") {
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
        os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
        val script =
         s"""#!/usr/bin/env bash
            |./scala ${TestUtil.extraOptions.mkString(" ") /* meh escaping */} $fileName | tee -a output
            |""".stripMargin
        os.write(root / "script.sh", script)
        os.perms.set(root / "script.sh", "rwxr-xr-x")
        os.proc("docker", "run", "--rm", "-v", s"${root}:/data", "-w", "/data", "ubuntu:18.04", "/data/script.sh").call(cwd = root)
        val output = os.read(root / "output").trim
        expect(output == message)
      }
    }

}
