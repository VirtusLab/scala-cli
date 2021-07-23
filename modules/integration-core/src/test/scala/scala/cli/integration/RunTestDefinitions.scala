package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.Charset

import scala.util.Properties

abstract class RunTestDefinitions(val scalaVersionOpt: Option[String]) extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

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
      val output = os.proc(TestUtil.cli, extraOptions, fileName).call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--js").call(cwd = root).out.text.trim
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
         s"""import scala.scalajs.js
            |val console = js.Dynamic.global.console
            |val msg = "$message"
            |console.log(msg)
            |""".stripMargin,
        os.rel / "scala.conf" ->
          """scala.platform = "js"
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, fileName, "--native").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--js").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, "print.sc", "messages.sc", "--native").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--main-class", "print").call(cwd = root).out.text.trim
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
            |""".stripMargin,
        os.rel / "dir" / "scala.conf" -> ""
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, "--main-class", "print").call(cwd = root / "dir").out.text.trim
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
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, "--", message).call(cwd = root).out.text.trim
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
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, "--", message).call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--js", "--main-class", "print").call(cwd = root).out.text.trim
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", "--native", "--main-class", "print").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (TestUtil.canRunNative && actualScalaVersion.startsWith("2."))
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
      val output = os.proc(TestUtil.cli, extraOptions, scriptPath.toString)
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
      val output = os.proc(TestUtil.cli, extraOptions, "dir", scriptPath.toString)
        .call(cwd = root)
        .out.text
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
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--java-prop", "scala.colored-stack-traces=false")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      // FIXME We need to have the pretty-stacktraces stuff take scala.colored-stack-traces into account
      val exceptionLines = res.out.lines.map(stripAnsi).dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val sp = " "
      val expectedLines =
        if (actualScalaVersion.startsWith("2.12."))
         s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
            |${tab}at Throws$$.main(Throws.scala:8)
            |${tab}at Throws.main(Throws.scala)
            |Caused by: java.lang.RuntimeException: nope
            |${tab}at scala.sys.package$$.error(package.scala:30)
            |${tab}at Throws$$.something(Throws.scala:3)
            |${tab}at Throws$$.main(Throws.scala:5)
            |${tab}... 1 more
            |""".stripMargin.linesIterator.toVector
        else if (actualScalaVersion.startsWith("2.13."))
         s"""Exception in thread "main" java.lang.Exception: Caught exception during processing
            |${tab}at Throws$$.main(Throws.scala:8)
            |${tab}at Throws.main(Throws.scala)
            |Caused by: java.lang.RuntimeException: nope
            |${tab}at scala.sys.package$$.error(package.scala:27)
            |${tab}at Throws$$.something(Throws.scala:3)
            |${tab}at Throws$$.main(Throws.scala:5)
            |${tab}... 1 more
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
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--java-prop", "scala.colored-stack-traces=false")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      val exceptionLines = res.out.lines.dropWhile(!_.startsWith("Exception in thread "))
      val tab = "\t"
      val expectedLines =
        if (actualScalaVersion.startsWith("2.12."))
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
        else
         s"""Exception in thread "main" java.lang.ExceptionInInitializerError
            |${tab}at throws.main(throws.sc)
            |Caused by: java.lang.Exception: Caught exception during processing
            |${tab}at throws$$.<clinit>(throws.sc:6)
            |${tab}... 1 more
            |Caused by: java.lang.RuntimeException: nope
            |${tab}at scala.sys.package$$.error(package.scala:27)
            |${tab}at throws$$.something(throws.sc:2)
            |${tab}at throws$$.<clinit>(throws.sc:3)
            |${tab}... 1 more
            |""".stripMargin.linesIterator.toVector
      if (exceptionLines != expectedLines) {
        pprint.log(exceptionLines)
        pprint.log(expectedLines)
      }
      expect(exceptionLines == expectedLines)
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
            |""".stripMargin,
        os.rel / "scala.conf" -> ""
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--runner=false")
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
      val cmd = s""" echo 'println("Hello" + " from pipe")' | ${TestUtil.cli.mkString(" ")} ${extraOptions.mkString(" ")} _.sc """
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
      val cmd = s""" ${TestUtil.cli.mkString(" ")} ${extraOptions.mkString(" ")} <(echo 'println("Hello" + " from fd")') """
      val res = os.proc("bash", "-c", cmd).call(cwd = root)
      val expectedOutput = "Hello from fd" + System.lineSeparator()
      expect(res.out.text == expectedOutput)
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
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/b0285fa0305f76856897517b06251970578565af/test.sc"
    val message = "Hello from GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Scala URL") {
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.scala"
    val message = "Hello from Scala GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  test("Java URL") {
    val url = "https://gist.github.com/alexarchambault/f972d941bc4a502d70267cfbbc4d6343/raw/2691c01984c9249936a625a42e29a822a357b0f6/Test.java"
    val message = "Hello from Java GitHub Gist"
    emptyInputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, escapedUrls(url)).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  def compileTimeOnlyJars(): Unit = {
    val shapelessJar = os.proc("cs", "fetch", "--intransitive", "com.chuusai::shapeless:2.3.7", "--scala", actualScalaVersion).call().out.text.trim
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
      val baseOutput = os.proc(TestUtil.cli, extraOptions, ".", "--extra-jar", shapelessJar).call(cwd = root).out.text.trim
      expect(baseOutput == "Hello with shapeless")
      val output = os.proc(TestUtil.cli, extraOptions, ".", "--compile-only-jar", shapelessJar).call(cwd = root).out.text.trim
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
      def run(warnAny: Boolean) =
        os.proc(TestUtil.cli, extraOptions, ".", if (warnAny) Seq("-Xlint:infer-any") else Nil).call(
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
        val res = os.proc(TestUtil.cli, "compile", extraOptions, "--class-path", ".", if (inlineDelambdafy) Seq("-Ydelambdafy:inline") else Nil)
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

  if (actualScalaVersion.startsWith("2."))
    test("Command-line -Y scalac options") {
      commandLineScalacYOption()
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
        val baseImage =
          if (TestUtil.cliKind == "native-static")
            Constants.dockerAlpineTestImage
          else
            Constants.dockerTestImage
        os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
        val script =
         s"""#!/usr/bin/env sh
            |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName | tee -a output
            |""".stripMargin
        os.write(root / "script.sh", script)
        os.perms.set(root / "script.sh", "rwxr-xr-x")
        val termOpt = if (System.console() == null) Nil else Seq("-t")
        os.proc("docker", "run", "--rm", termOpt, "-v", s"${root}:/data", "-w", "/data", baseImage, "/data/script.sh").call(cwd = root)
        val output = os.read(root / "output").trim
        expect(output == message)
      }
    }

  test("Java options in config file") {
    val message = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "simple.sc" ->
          """val msg = sys.props("test.message")
            |println(msg)
            |""".stripMargin,
        os.rel / "scala.conf" ->
         s"""java.options = ["-Dtest.message=$message"]
            |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  def simpleScriptDistrolessImage(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
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
      os.proc("docker", "build", "-t", "scala-cli-distroless-it", ".").call(cwd = root)
      os.remove(root / "scala")
      os.remove(root / "Dockerfile")
      val termOpt = if (System.console() == null) Nil else Seq("-t")
      val rawOutput = new ByteArrayOutputStream
      os.proc("docker", "run", "--rm", termOpt, "-v", s"$root:/data", "-w", "/data", "scala-cli-distroless-it", extraOptions, fileName).call(
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

}
