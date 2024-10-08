package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{ByteArrayOutputStream, File}
import java.nio.charset.Charset

import scala.cli.integration.util.DockerServer
import scala.io.Codec
import scala.jdk.CollectionConverters.*
import scala.util.Properties

abstract class RunTestDefinitions
    extends WithWarmUpScalaCliSuite
    with TestScalaVersionArgs
    with RunScriptTestDefinitions
    with RunScalaJsTestDefinitions
    with RunScalaNativeTestDefinitions
    with RunPipedSourcesTestDefinitions
    with RunGistTestDefinitions
    with RunScalacCompatTestDefinitions
    with RunSnippetTestDefinitions
    with RunScalaPyTestDefinitions
    with RunZipTestDefinitions { _: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions
  protected val emptyInputs: TestInputs        = TestInputs(os.rel / ".placeholder" -> "")

  override def warmUpExtraTestOptions: Seq[String] = extraOptions

  protected val ciOpt: Seq[String] =
    Option(System.getenv("CI")).map(v => Seq("-e", s"CI=$v")).getOrElse(Nil)

  test("print command") {
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
        os.proc(TestUtil.cli, extraOptions, fileName, "--command").call(cwd = root).out.trim()
      val command      = output.linesIterator.toVector
      val actualOutput = os.proc(command).call(cwd = root).out.trim()
      expect(actualOutput == message)
    }
  }

  test("manifest") {
    val message = "Hello"
    val converters =
      if (actualScalaVersion.startsWith("2.12.")) "scala.collection.JavaConverters._"
      else "scala.jdk.CollectionConverters._"
    val inputs = TestInputs(
      os.rel / "Simple.scala" ->
        s"""import java.io.File
           |import java.util.zip.ZipFile
           |import $converters
           |
           |object Simple {
           |  private def manifestClassPathCheck(): Unit = {
           |    val cp = sys.props("java.class.path")
           |    assert(!cp.contains(File.pathSeparator), s"Expected single entry in class path, got $$cp")
           |    val zf = new ZipFile(new File(cp))
           |    val entries = zf.entries.asScala.map(_.getName).toVector
           |    zf.close()
           |    assert(entries == Seq("META-INF/MANIFEST.MF"), s"Expected only META-INF/MANIFEST.MF entry, got $$entries")
           |  }
           |  def main(args: Array[String]): Unit = {
           |    manifestClassPathCheck()
           |    val msg = "$message"
           |    println(msg)
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, "--use-manifest", ".")
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }

  def platformNl: String = if (Properties.isWin) "\\r\\n" else "\\n"

  test("No default inputs when the `run` sub-command is launched with no args") {
    val inputs = TestInputs(
      os.rel / "dir" / "print.sc" ->
        s"""println("Foo")
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, "--main-class", "print")
        .call(cwd = root / "dir", check = false, mergeErrIntoOut = true)
      val output = res.out.trim()
      expect(res.exitCode != 0)
      expect(output.contains("No inputs provided"))
    }
  }

  test("Debugging") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""object Foo {
           |  def main(args: Array[String]): Unit = {
           |    println("foo")
           |  }
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val out1 = os.proc(TestUtil.cli, "run", extraOptions, ".", "--debug", "--command")
        .call(cwd = root).out.trim().lines.toList.asScala
      val out2 = os.proc(
        TestUtil.cli,
        "run",
        extraOptions,
        ".",
        "--debug-port",
        "5006",
        "--debug-mode",
        "listen",
        "--command"
      ).call(cwd = root).out.trim().lines.toList.asScala

      def debugString(server: String, port: String) =
        s"-agentlib:jdwp=transport=dt_socket,server=$server,suspend=y,address=$port"

      assert(out1.contains(debugString("y", "5005")))
      assert(out2.contains(debugString("n", "5006")))
    }
  }

  test("Pass arguments") {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        s"""object Test {
           |  def main(args: Array[String]): Unit = {
           |    println(args(0))
           |  }
           |}
           |""".stripMargin
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, ".", "--", message)
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }

  def passArgumentsScala3(): Unit = {
    val inputs = TestInputs(
      os.rel / "Test.scala" ->
        s"""object Test:
           |  def main(args: Array[String]): Unit =
           |    val message = args(0)
           |    println(message)
           |""".stripMargin
    )
    val message = "Hello"
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, "run", extraOptions, ".", "--", message)
        .call(cwd = root)
        .out.trim()
      expect(output == message)
    }
  }

  if (actualScalaVersion.startsWith("3."))
    test("Pass arguments - Scala 3") {
      passArgumentsScala3()
    }

  test("setting root dir with virtual input") {
    val url = "https://gist.github.com/alexarchambault/7b4ec20c4033690dd750ffd601e540ec"
    emptyInputs.fromRoot { root =>
      os.proc(TestUtil.cli, extraOptions, escapedUrls(url)).call(cwd = root)
      expect(
        !os.exists(root / ".scala-build")
      ) // virtual source should not create workspace dir in cwd
    }
  }

  private lazy val ansiRegex = "\u001B\\[[;\\d]*m".r

  protected def stripAnsi(s: String): String = ansiRegex.replaceAllIn(s, "")

  test("stack traces") {
    val inputs = TestInputs(
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
    inputs.fromRoot { root =>
      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "run",
        extraOptions,
        ".",
        "--java-prop",
        "scala.colored-stack-traces=false"
      )
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

  def compileTimeOnlyJars(): Unit = {
    val cmd = Seq[os.Shellable](
      TestUtil.cs,
      "fetch",
      "--intransitive",
      "com.chuusai::shapeless:2.3.9",
      "--scala",
      actualScalaVersion
    )
    val shapelessJar = os.proc(cmd).call().out.trim()
    expect(os.isFile(os.Path(shapelessJar, os.pwd)))

    val inputs = TestInputs(
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
    inputs.fromRoot { root =>
      val baseOutput = os.proc(TestUtil.cli, extraOptions, ".", "--extra-jar", shapelessJar)
        .call(cwd = root)
        .out.trim()
      expect(baseOutput == "Hello with shapeless")
      val output = os.proc(TestUtil.cli, extraOptions, ".", "--compile-only-jar", shapelessJar)
        .call(cwd = root)
        .out.trim()
      expect(output == "Hello from test")
    }
  }

  // TODO Adapt this test to Scala 3
  if (actualScalaVersion.startsWith("2."))
    test("Compile-time only JARs") {
      compileTimeOnlyJars()
    }

  test("compile-time only for jsoniter macros") {
    val inputs = TestInputs(
      os.rel / "hello.sc" ->
        """|//> using lib "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-core:2.23.2"
           |//> using compileOnly.lib "com.github.plokhotnyuk.jsoniter-scala::jsoniter-scala-macros:2.23.2"
           |
           |import com.github.plokhotnyuk.jsoniter_scala.core._
           |import com.github.plokhotnyuk.jsoniter_scala.macros._
           |
           |case class User(name: String, friends: Seq[String])
           |implicit val codec: JsonValueCodec[User] = JsonCodecMaker.make
           |
           |val user = readFromString[User]("{\"name\":\"John\",\"friends\":[\"Mark\"]}")
           |System.out.println(user.name)
           |val classPath = System.getProperty("java.class.path").split(java.io.File.pathSeparator).iterator.toList
           |System.out.println(classPath)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".")
        .call(cwd = root)
        .out.trim()
      expect(output.contains("John"))
      expect(!output.contains("jsoniter-scala-macros"))
    }
  }

  def compileTimeOnlyDep(): Unit = {

    def inputs(compileOnly: Boolean) = {
      val directiveName = if (compileOnly) "compileOnly.dep" else "dep"
      TestInputs(
        os.rel / "test.sc" ->
          s"""//> using $directiveName "com.chuusai::shapeless:2.3.10"
             |val shapelessFound =
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
    }

    inputs(compileOnly = false).fromRoot { root =>
      val baseOutput = os.proc(TestUtil.cli, extraOptions, ".")
        .call(cwd = root)
        .out.trim()
      expect(baseOutput == "Hello with shapeless")
    }
    inputs(compileOnly = true).fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".")
        .call(cwd = root)
        .out.trim()
      expect(output == "Hello from test")
    }
  }

  if (actualScalaVersion.startsWith("2."))
    test("Compile-time only dep") {
      compileTimeOnlyDep()
    }

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("no JVM installed") {
      val fileName = "simple.sc"
      val message  = "Hello"
      val inputs = TestInputs(
        os.rel / fileName ->
          s"""val msg = "$message"
             |println(msg)
             |""".stripMargin
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
             |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName| tee -a output
             |""".stripMargin
        os.write(root / "script.sh", script)
        os.perms.set(root / "script.sh", "rwxr-xr-x")
        val termOpt = if (System.console() == null) Nil else Seq("-t")
        val cmd = Seq[os.Shellable](
          "docker",
          "run",
          "--rm",
          termOpt,
          "-v",
          s"$root:/data",
          "-w",
          "/data",
          ciOpt,
          baseImage,
          "/data/script.sh"
        )
        val res = os.proc(cmd).call(cwd = root)
        System.err.println(res.out.text())
        val output = os.read(root / "output").trim
        expect(output == message)
      }
    }

  test("Java options in config file") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "simple.sc" ->
        s"""//> using javaOpt "-Dtest.message=$message"
           |val msg = sys.props("test.message")
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("Main class in config file") {
    val inputs = TestInputs(
      os.rel / "simple.scala" ->
        s"""//> using `main-class` "hello"
           |object hello extends App { println("hello") }
           |object world extends App { println("world") }
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root).out.trim()
      expect(output == "hello")
    }
  }

  def simpleScriptDistrolessImage(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin,
      os.rel / "Dockerfile" ->
        os.read(os.Path(Constants.mostlyStaticDockerfile, os.pwd))
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
      val cmd = Seq[os.Shellable](
        "docker",
        "run",
        "--rm",
        termOpt,
        "-v",
        s"$root:/data",
        "-w",
        "/data",
        ciOpt,
        "scala-cli-distroless-it",
        extraOptions,
        fileName
      )
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
    os.rel / "dir" / "Hello.scala" ->
      """object Hello {
        |  def main(args: Array[String]): Unit = {
        |    val p = java.nio.file.Paths.get(getClass.getProtectionDomain.getCodeSource.getLocation.toURI)
        |    println(p)
        |  }
        |}
        |""".stripMargin
  )

  private def nonWritableTest(): Unit = {
    simpleDirInputs.fromRoot { root =>
      def run(): String = {
        val res = os.proc(TestUtil.cli, "dir").call(cwd = root)
        res.out.trim()
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
        res.out.trim()
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

  protected def resourcesInputs(
    directive: String = "",
    resourceContent: String = "Hello from resources"
  ): TestInputs =
    TestInputs(
      os.rel / "src" / "proj" / "resources" / "test" / "data" -> resourceContent,
      os.rel / "src" / "proj" / "Test.scala" ->
        s"""$directive
           |object Test {
           |  def main(args: Array[String]): Unit = {
           |    val cl = Thread.currentThread().getContextClassLoader
           |    val is = cl.getResourceAsStream("test/data")
           |    val content = scala.io.Source.fromInputStream(is)(scala.io.Codec.UTF8).mkString
           |    println(content)
           |  }
           |}
           |""".stripMargin
    )

  test("resources via command line") {
    val expectedMessage = "hello"
    resourcesInputs(resourceContent = expectedMessage).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", "src", "--resource-dirs", "./src/proj/resources")
        .call(cwd = root)
      expect(res.out.trim() == expectedMessage)
    }
  }
  test("resources via directive") {
    val expectedMessage = "hello"
    resourcesInputs(
      directive = "//> using resourceDirs \"./resources\"",
      resourceContent = expectedMessage
    )
      .fromRoot { root =>
        val res = os.proc(TestUtil.cli, "run", ".").call(cwd = root)
        expect(res.out.trim() == expectedMessage)
      }
  }

  def argsAsIsTest(): Unit = {
    val inputs = TestInputs(
      os.rel / "MyScript.scala" ->
        """#!/usr/bin/env -S scala-cli shebang
          |object MyScript {
          |  def main(args: Array[String]): Unit =
          |    println("Hello" + args.map(" " + _).mkString)
          |}
          |""".stripMargin
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
      expect(res.out.trim() == "Hello from tests")
    }
  }

  if (TestUtil.isNativeCli && !Properties.isWin)
    test("should pass arguments as is") {
      argsAsIsTest()
    }

  test("test scope") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using dep "com.lihaoyi::utest:0.7.10"
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
      os.rel / "Tests.test.scala" ->
        """//> using dep "com.lihaoyi::pprint:0.6.6"
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
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root)
      pprint.log(res.out.text())
      expect(res.out.text().contains("Hello from tests"))
    }
  }

  if (!Properties.isWin)
    test("CLI args passed to shebang in Scala file") {
      val inputs = TestInputs(
        os.rel / "f.scala" ->
          s"""|#!/usr/bin/env -S ${TestUtil.cli.mkString(" ")} shebang
              |object Hello {
              |    def main(args: Array[String]) = {
              |        println(args.toList)
              |    }
              |}
              |""".stripMargin
      )
      inputs.fromRoot { root =>
        os.perms.set(root / "f.scala", os.PermSet.fromString("rwx------"))
        val p = os.proc("./f.scala", "1", "2", "3", "-v").call(cwd = root)
        expect(p.out.trim() == "List(1, 2, 3, -v)")
      }
    }

  test("Runs with JVM 8") {
    val inputs =
      TestInputs(
        os.rel / "run.scala" -> """object Main extends App { println(System.getProperty("java.version"))}"""
      )
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "run.scala", "--jvm", "8").call(cwd = root)
      expect(p.out.trim().startsWith("1.8"))
    }
  }

  test("Runs with JVM 8 with using directive") {
    val inputs =
      TestInputs(os.rel / "run.scala" ->
        """//> using jvm "8"
          |object Main extends App { println(System.getProperty("java.version"))}""".stripMargin)
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "run.scala").call(cwd = root)
      expect(p.out.trim().startsWith("1.8"))
    }
  }

  test("workspace dir") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """|//> using dep "com.lihaoyi::os-lib:0.7.8"
           |
           |object Hello extends App {
           |  println(os.pwd)
           |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val p = os.proc(TestUtil.cli, "Hello.scala").call(cwd = root)
      expect(p.out.trim() == root.toString)
    }
  }

  test("-D.. options passed to the child app") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object ClassHello extends App {
          |  print(System.getProperty("foo"))
          |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "Hello.scala", "--java-opt", "-Dfoo=bar").call(
        cwd = root
      )
      expect(res.out.trim() == "bar")
    }
  }

  test("java style -Dproperty=value system properties") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello extends App {
          |  print(System.getProperty("foo"))
          |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "Hello.scala", "-Dfoo=bar").call(
        cwd = root
      )
      expect(res.out.trim() == "bar")
    }
  }

  test("add to class path sources from using directive") {
    val fileName       = "Hello.scala"
    val (hello, world) = ("Hello", "World")
    val inputs = TestInputs(
      os.rel / fileName ->
        """|//> using file "Utils.scala", "helper"
           |
           |object Hello extends App {
           |   println(s"${Utils.hello}${helper.Helper.world}")
           |}""".stripMargin,
      os.rel / "Utils.scala" ->
        s"""|object Utils {
            |  val hello = "$hello"
            |}""".stripMargin,
      os.rel / "helper" / "Helper.scala" ->
        s"""|package helper
            |object Helper {
            |  val world = "$world"
            |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "Hello.scala")
        .call(cwd = root)
      expect(res.out.trim() == s"$hello$world")
    }
  }

  test("multiple using directives warning message") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using scala "3.2.0"
           |
           |object Foo extends App {
           |  println("Foo")
           |}
           |""".stripMargin,
      os.rel / "Bar.scala"  -> "",
      os.rel / "Hello.java" -> "//> using jvm \"11\""
    )
    inputs.fromRoot { root =>
      val warningMessage =
        """Using directives detected in multiple files:
          |- Foo.scala:1:1-24
          |- Hello.java:1:1-19""".stripMargin
      val output1 = os.proc(TestUtil.cli, ".").call(cwd = root, stderr = os.Pipe).err.trim()
      val output2 = os.proc(TestUtil.cli, "Foo.scala", "Bar.scala").call(
        cwd = root,
        stderr = os.Pipe
      ).err.trim()
      expect(output1.contains(warningMessage))
      expect(!output2.contains("Using directives detected in multiple files"))
    }
  }

  test("suppress multiple using directives warning message") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using scala "3.2.0"
           |
           |object Foo extends App {
           |  println("Foo")
           |}
           |""".stripMargin,
      os.rel / "Bar.scala"  -> "",
      os.rel / "Hello.java" -> "//> using jvm \"11\""
    )
    inputs.fromRoot { root =>
      val warningMessage = "Using directives detected in"
      val output =
        os.proc(TestUtil.cli, ".", "--suppress-warning-directives-in-multiple-files").call(
          cwd = root,
          stderr = os.Pipe
        ).err.trim()
      expect(!output.contains(warningMessage))
    }
  }

  test("suppress multiple using directives warning message with global config") {
    val inputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using scala "3.2.0"
           |
           |object Foo extends App {
           |  println("Foo")
           |}
           |""".stripMargin,
      os.rel / "Bar.scala"  -> "",
      os.rel / "Hello.java" -> "//> using jvm \"11\""
    )
    inputs.fromRoot { root =>
      val warningMessage = "Using directives detected in"

      os.proc(TestUtil.cli, "config", "suppress-warning.directives-in-multiple-files", "true")
        .call(cwd = root)

      val output =
        os.proc(TestUtil.cli, ".").call(
          cwd = root,
          stderr = os.Pipe
        ).err.trim()
      expect(!output.contains(warningMessage))

      os.proc(TestUtil.cli, "config", "suppress-warning.directives-in-multiple-files", "false")
        .call(cwd = root)
    }
  }

  def sudoTest(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
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
           |./scala ${extraOptions.mkString(" ") /* meh escaping */} $fileName| tee output-root
           |sudo -u test ./scala clean $fileName
           |sudo -u test ./scala ${
            extraOptions.mkString(
              " "
            ) /* meh escaping */
          } $fileName| tee output-user
           |""".stripMargin
      os.write(root / "script.sh", script)
      os.perms.set(root / "script.sh", "rwxr-xr-x")
      val termOpt = if (System.console() == null) Nil else Seq("-t")
      val cmd = Seq[os.Shellable](
        "docker",
        "run",
        "--rm",
        termOpt,
        "-v",
        s"$root:/data",
        "-w",
        "/data",
        ciOpt,
        baseImage,
        "/data/script.sh"
      )
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

  def authProxyTest(legacySetup: Boolean): Unit = {
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
      }*
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

    val proxyArgs =
      if (legacySetup) authProperties("localhost", 9083, "jack", "insecure")
      else Nil
    val wrongProxyArgs =
      if (legacySetup) authProperties("localhost", 9084, "wrong", "nope")
      else Nil

    def setupProxyConfig(
      cwd: os.Path,
      env: Map[String, String],
      host: String,
      port: Int,
      user: String,
      password: String
    ): Unit = {
      os.proc(TestUtil.cli, "--power", "config", "httpProxy.address", s"http://$host:$port")
        .call(cwd = cwd, env = env)
      os.proc(TestUtil.cli, "--power", "config", "httpProxy.user", s"value:$user")
        .call(cwd = cwd, env = env)
      os.proc(TestUtil.cli, "--power", "config", "httpProxy.password", s"value:$password")
        .call(cwd = cwd, env = env)
    }

    val image = Constants.authProxyTestImage
    inputs.fromRoot { root =>
      val configDir = root / "configs"
      os.makeDir(configDir, "rwx------")
      val configFile      = configDir / "config.json"
      val wrongConfigFile = configDir / "wrong-config.json"
      val (configEnv, wrongConfigEnv) =
        if (legacySetup)
          (Map.empty[String, String], Map.empty[String, String])
        else {
          val csEnv           = TestUtil.putCsInPathViaEnv(root / "bin")
          val configEnv0      = Map("SCALA_CLI_CONFIG" -> configFile.toString) ++ csEnv
          val wrongConfigEnv0 = Map("SCALA_CLI_CONFIG" -> wrongConfigFile.toString) ++ csEnv
          setupProxyConfig(root, configEnv0, "localhost", 9083, "jack", "insecure")
          setupProxyConfig(root, wrongConfigEnv0, "localhost", 9084, "wrong", "nope")
          (configEnv0, wrongConfigEnv0)
        }
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
            .call(cwd = root / okDir, env = configEnv)
          val okOutput = okRes.out.trim()
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
            .call(
              cwd = root / wrongDir,
              env = wrongConfigEnv,
              mergeErrIntoOut = true,
              check = false
            )
          val wrongOutput = wrongRes.out.trim()
          expect(wrongRes.exitCode == 1)
          expect(wrongOutput.contains(
            """Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required""""
          ))
        }
      }
    }
  }

  def runAuthProxyTests: Boolean =
    Properties.isLinux || (Properties.isMac && !TestUtil.isCI)

  if (runAuthProxyTests) {
    test("auth proxy (legacy)") {
      TestUtil.retry() {
        authProxyTest(legacySetup = true)
      }
    }

    test("auth proxy") {
      TestUtil.retry() {
        authProxyTest(legacySetup = false)
      }
    }
  }

  test("UTF-8") {
    val message  = "Hello from TestÅÄÖåäö"
    val fileName = "TestÅÄÖåäö.scala"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""object TestÅÄÖåäö {
           |  def main(args: Array[String]): Unit = {
           |    println("$message")
           |  }
           |}
           |""".stripMargin
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

  test("return relevant error if multiple .scala main classes are present") {
    val (scalaFile1, scalaFile2, scriptName) = ("ScalaMainClass1", "ScalaMainClass2", "ScalaScript")
    val scriptsDir                           = "scripts"
    val inputs = TestInputs(
      os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
      os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
      os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        ".",
        extraOptions
      )
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode == 1)
      val output = res.out.trim()
      val errorMessage =
        output.linesWithSeparators.toSeq.takeRight(6).mkString // dropping compilation logs
      val extraOptionsString = extraOptions.mkString(" ")
      val scriptMainClassName = if (actualScalaVersion.startsWith("3"))
        s"$scriptsDir.${scriptName}_sc"
      else
        s"$scriptsDir.$scriptName"

      val expectedMainClassNames = Seq(scalaFile1, scalaFile2, scriptMainClassName).sorted
      val expectedErrorMessage =
        s"""[${Console.RED}error${Console.RESET}]  Found several main classes: ${
            expectedMainClassNames.mkString(
              ", "
            )
          }
           |You can run one of them by passing it with the --main-class option, e.g.
           |  ${Console.BOLD}${TestUtil.detectCliPath} run . $extraOptionsString --main-class ${expectedMainClassNames
            .head}${Console.RESET}
           |
           |You can pick the main class interactively by passing the --interactive option.
           |  ${Console.BOLD}${TestUtil
            .detectCliPath} run . $extraOptionsString --interactive${Console.RESET}""".stripMargin
      expect(errorMessage == expectedErrorMessage)
    }
  }

  test(
    "return relevant error when main classes list is requested, but no main classes are present"
  ) {
    val inputs = TestInputs(os.rel / "Main.scala" -> "object Main { println() }")
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
      expect(res.out.trim().contains("No main class found"))
    }
  }

  test("correctly list main classes") {
    val (scalaFile1, scalaFile2, scriptName) = ("ScalaMainClass1", "ScalaMainClass2", "ScalaScript")
    val scriptsDir                           = "scripts"
    val inputs = TestInputs(
      os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
      os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
      os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
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
      val output      = res.out.trim()
      val mainClasses = output.split(" ").toSet

      val scriptMainClassName = if (actualScalaVersion.startsWith("3"))
        s"$scriptsDir.${scriptName}_sc"
      else
        s"$scriptsDir.$scriptName"

      expect(mainClasses == Set(scalaFile1, scalaFile2, scriptMainClassName))
    }
  }

  test("deleting resources after building") {
    val projectDir      = "projectDir"
    val fileName        = "main.scala"
    val resourceContent = "hello world"
    val resourcePath    = os.rel / projectDir / "resources" / "test.txt"
    val inputs = TestInputs(
      os.rel / projectDir / fileName ->
        s"""
           |//> using resourceDir "resources"
           |
           |object Main {
           |  def main(args: Array[String]) = {
           |    val inputStream = getClass().getResourceAsStream("/test.txt")
           |    if (inputStream == null) println("null")
           |    else println("non null")
           |  }
           |}
           |""".stripMargin,
      resourcePath -> resourceContent
    )

    inputs.fromRoot { root =>
      def runCli() =
        os.proc(TestUtil.cli, extraOptions, projectDir)
          .call(cwd = root)
          .out.trim()

      val output1 = runCli()
      expect(output1 == "non null")

      os.remove(root / resourcePath)
      val output2 = runCli()
      expect(output2 == "null")
    }
  }

  test("run jar file") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           |  println("Hello World")
           |}""".stripMargin
    )
    inputs.fromRoot { root =>
      // build jar
      val helloJarPath = root / "Hello.jar"
      os.proc(TestUtil.cli, "--power", "package", ".", "--library", "-o", helloJarPath).call(cwd =
        root
      )

      // run jar
      val output = os.proc(TestUtil.cli, helloJarPath).call(cwd = root).out.trim()
      expect(output == "Hello World")
    }
  }

  if (actualScalaVersion.startsWith("3"))
    test("should throw exception for code compiled by scala 3.1.3") {
      val exceptionMsg = "Throw exception in Scala"
      val inputs = TestInputs(
        os.rel / "hello.sc" ->
          s"""//> using scala "3.1.3"
             |throw new Exception("$exceptionMsg")""".stripMargin
      )

      inputs.fromRoot { root =>
        val res =
          os.proc(TestUtil.cli, "hello.sc").call(cwd = root, mergeErrIntoOut = true, check = false)
        val output = res.out.trim()
        expect(output.contains(exceptionMsg))
      }
    }

  test("should add toolkit to classpath") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           |  println(os.pwd) // os lib should be added to classpath by toolkit
           |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(TestUtil.cli, ".", "--toolkit", Constants.toolkitVersion)
        .call(cwd = root).out.trim()

      expect(output == root.toString())
    }
  }

  test("should add typelevel toolkit to classpath") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""|import cats.effect.*
            |import fs2.io.file.Files
            |object Hello extends IOApp.Simple {
            |  // IO should be added to classpath by typelevel toolkit
            |  def run = Files[IO].currentWorkingDirectory.flatMap { cwd =>
            |    IO.println(cwd.toString)
            |  }
            |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val output =
        os.proc(TestUtil.cli, ".", "--toolkit", s"typelevel:${Constants.typelevelToolkitVersion}")
          .call(cwd = root).out.trim()

      expect(output == root.toString())
    }
  }

  test("should add typelevel toolkit-test to classpath") {
    val inputs = TestInputs(
      os.rel / "Hello.test.scala" ->
        s"""|import cats.effect.*
            |import munit.CatsEffectSuite
            |class HelloSuite extends CatsEffectSuite {
            |  // IO should be added to classpath by typelevel toolkit-test
            |  test("warm hello from the sun is coming") {
            |    (IO("i love to live in the") *> IO("sun")).assertEquals("sun")
            |  }
            |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val output = os.proc(
        TestUtil.cli,
        "test",
        ".",
        "--toolkit",
        s"typelevel:${Constants.typelevelToolkitVersion}"
      )
        .call(cwd = root).out.text()

      expect(output.contains("+")) // test succeeded
    }
  }

  test(s"print error if workspace path contains a ${File.pathSeparator}") {
    val msg     = "Hello"
    val relPath = os.rel / s"weird${File.pathSeparator}directory" / "Hello.scala"
    TestInputs(
      relPath ->
        s"""object Hello extends App {
           |  println("$msg")
           |}
           |""".stripMargin
    )
      .fromRoot { root =>
        val resWithColon =
          os.proc(TestUtil.cli, "run", relPath.toString, extraOptions)
            .call(cwd = root, check = false, stderr = os.Pipe)
        expect(resWithColon.exitCode == 1)
        expect(resWithColon.err.trim().contains(
          "you can force your workspace with the '--workspace' option:"
        ))
        val resFixedWorkspace = // should run fine for a forced workspace with no classpath separator on path
          os.proc(TestUtil.cli, "run", relPath.toString, "--workspace", ".", extraOptions)
            .call(cwd = root)
        expect(resFixedWorkspace.out.trim() == msg)
      }
  }

  val commands = Seq("", "run", "compile")

  for (command <- commands) {
    test(
      s"error output for unrecognized source type for ${if (command == "") "default" else command}"
    ) {
      val inputs = TestInputs(
        os.rel / "print.hehe" ->
          s"""println("Foo")
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        val proc = if (command == "") os.proc(TestUtil.cli, "print.hehe")
        else os.proc(TestUtil.cli, command, "print.hehe")
        val output = proc.call(cwd = root, check = false, stderr = os.Pipe)
          .err.trim()

        expect(output.contains("unrecognized source type"))
      }
    }

    test(s"error output for nonexistent file for ${if (command == "") "default" else command}") {
      val inputs = TestInputs(
        os.rel / "print.hehe" ->
          s"""println("Foo")
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        val proc = if (command == "") os.proc(TestUtil.cli, "nonexisten.no")
        else os.proc(TestUtil.cli, command, "nonexisten.no")
        val output = proc.call(cwd = root, check = false, stderr = os.Pipe)
          .err.trim()

        expect(output.contains("file not found"))
      }
    }

    test(s"error output for invalid sub-command for ${if (command == "") "default" else command}") {
      val inputs = TestInputs(
        os.rel / "print.hehe" ->
          s"""println("Foo")
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        val proc = if (command == "") os.proc(TestUtil.cli, "invalid")
        else os.proc(TestUtil.cli, command, "invalid")
        val output = proc.call(cwd = root, check = false, stderr = os.Pipe)
          .err.trim()

        if (command == "")
          expect(output.contains(s"is not a ${TestUtil.detectCliPath} sub-command"))
        else
          expect(output.contains("file not found"))
      }
    }
  }

  test("declare test scope dependencies from main scope") {
    val projectFile     = "project.scala"
    val invalidMainFile = "InvalidMain.scala"
    val validMainFile   = "ValidMain.scala"
    val testFile        = "Tests.test.scala"
    TestInputs(
      os.rel / projectFile ->
        """//> using dep "com.lihaoyi::os-lib:0.9.1"
          |//> using test.dep "org.scalameta::munit::0.7.29"
          |//> using test.dep "com.lihaoyi::pprint:0.8.1"
          |""".stripMargin,
      os.rel / invalidMainFile ->
        """object InvalidMain extends App {
          |  pprint.pprintln("Hello")
          |}
          |""".stripMargin,
      os.rel / validMainFile ->
        """object ValidMain extends App {
          |  println(os.pwd)
          |}
          |""".stripMargin,
      os.rel / testFile ->
        """class Tests extends munit.FunSuite {
          |  test("foo") {
          |    pprint.pprintln(os.pwd)
          |  }
          |}
          |""".stripMargin
    ).fromRoot { root =>
      // running `invalidMainFile` should fail, as it's in the main scope and depends on test scope deps
      val res1 = os.proc(TestUtil.cli, "run", projectFile, invalidMainFile)
        .call(cwd = root, check = false)
      expect(res1.exitCode == 1)
      // running `validMainFile` should succeed, since it only depends on main scope deps
      val res2 = os.proc(TestUtil.cli, "run", projectFile, validMainFile)
        .call(cwd = root)
      expect(res2.out.trim() == root.toString())
      // test scope should have access to both main and test deps
      os.proc(TestUtil.cli, "test", projectFile, testFile)
        .call(cwd = root, stderr = os.Pipe)
    }
  }
  test("declare test scope custom jar from main scope") {
    val projectFile      = "project.scala"
    val testMessageFile  = "TestMessage.scala"
    val mainMessageFile  = "MainMessage.scala"
    val validMainFile    = "ValidMain.scala"
    val invalidMainFile  = "InvalidMain.scala"
    val testFile         = "Tests.test.scala"
    val expectedMessage1 = "Hello"
    val expectedMessage2 = " world!"
    val jarPathsWithFiles @ Seq((mainMessageJar, _), (testMessageJar, _)) =
      Seq(
        os.rel / "MainMessage.jar" -> mainMessageFile,
        os.rel / "TestMessage.jar" -> testMessageFile
      )
    TestInputs(
      os.rel / projectFile ->
        s"""//> using jar "$mainMessageJar"
           |//> using test.jar "$testMessageJar"
           |//> using test.dep "org.scalameta::munit::0.7.29"
           |""".stripMargin,
      os.rel / mainMessageFile ->
        """case class MainMessage(value: String)
          |""".stripMargin,
      os.rel / testMessageFile ->
        """case class TestMessage(value: String)
          |""".stripMargin,
      os.rel / invalidMainFile ->
        s"""object InvalidMain extends App {
           |  println(TestMessage("$expectedMessage1").value)
           |}
           |""".stripMargin,
      os.rel / validMainFile ->
        s"""object ValidMain extends App {
           |  println(MainMessage("$expectedMessage1").value)
           |}
           |""".stripMargin,
      os.rel / testFile ->
        s"""class Tests extends munit.FunSuite {
           |  val msg1 = MainMessage("$expectedMessage1").value
           |  val msg2 = TestMessage("$expectedMessage2").value
           |  val testName = msg1 + msg2
           |  test(testName) {
           |    assert(1 + 1 == 2)
           |  }
           |}
           |""".stripMargin
    ).fromRoot { root =>
      // package the MainMessage and TestMessage jars
      for ((jarPath, sourcePath) <- jarPathsWithFiles)
        os.proc(
          TestUtil.cli,
          "--power",
          "package",
          sourcePath,
          "--library",
          "-o",
          jarPath,
          extraOptions
        )
          .call(cwd = root)
      // running `invalidMainFile` should fail, as it's in the main scope and depends on the test scope jar
      val res1 = os.proc(TestUtil.cli, "run", projectFile, invalidMainFile, extraOptions)
        .call(cwd = root, check = false)
      expect(res1.exitCode == 1)
      // running `validMainFile` should succeed, since it only depends on the main scope jar
      val res2 = os.proc(TestUtil.cli, "run", projectFile, validMainFile, extraOptions)
        .call(cwd = root)
      expect(res2.out.trim() == expectedMessage1)
      // test scope should have access to both main and test deps
      val res3 = os.proc(TestUtil.cli, "test", projectFile, testFile, extraOptions)
        .call(cwd = root, stderr = os.Pipe)
      expect(res3.out.trim().contains(s"$expectedMessage1$expectedMessage2"))
    }
  }
  test("exclude file") {
    val message = "Hello"
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello extends App {
           | println("$message")
           |}""".stripMargin,
      os.rel / "Main.scala" ->
        """object Main {
          | val msg: String = 1 // compilation fails
          |}""".stripMargin
    )
    inputs.fromRoot { root =>
      val res =
        os.proc(TestUtil.cli, extraOptions, ".", "--exclude", "*Main.scala").call(cwd = root)
      val output = res.out.trim()
      expect(output == message)
    }
  }

  test("decoded classNames in interactive ask") {
    val fileName = "watch.scala"

    val inputs = TestInputs(
      os.rel / fileName ->
        """object `Run-1` extends App {println("Run-1 launched")}
          |object `Run-2` extends App {println("Run-2 launched")}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val confDir  = root / "config"
      val confFile = confDir / "test-config.json"

      os.write(confFile, "{\"interactive-was-suggested\":true}", createFolders = true)

      if (!Properties.isWin)
        os.perms.set(confDir, "rwx------")

      val configEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      val proc = os.proc(TestUtil.cli, "run", "--interactive", fileName)
        .call(
          cwd = root,
          mergeErrIntoOut = true,
          env = Map("SCALA_CLI_INTERACTIVE_INPUTS" -> "Run-1") ++ configEnv
        )

      expect(proc.out.trim().contains("[0] Run-1"))
      expect(proc.out.trim().contains("[1] Run-2"))
      expect(proc.out.trim().contains("Run-1 launched"))
    }
  }

  test("BuildInfo fields should be reachable") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using dep com.lihaoyi::os-lib:0.9.1
           |//> using option -Xasync
           |//> using jvm 11
           |//> using mainClass Main
           |//> using resourceDir ./resources
           |//> using jar TEST1.jar TEST2.jar
           |
           |//> using buildInfo
           |
           |import scala.cli.build.BuildInfo
           |
           |object Main extends App {
           |  assert(BuildInfo.scalaVersion == "$actualScalaVersion")
           |  assert(BuildInfo.platform == "JVM")
           |  assert(BuildInfo.jvmVersion == Some("11"))
           |  assert(BuildInfo.scalaJsVersion == None)
           |  assert(BuildInfo.jsEsVersion == None)
           |  assert(BuildInfo.scalaNativeVersion == None)
           |  assert(BuildInfo.mainClass == Some("Main"))
           |  assert(BuildInfo.projectVersion == Some("1.0.0"))
           |
           |  assert(BuildInfo.Main.sources.head.endsWith("Main.scala"))
           |  assert(BuildInfo.Main.scalacOptions == Seq("-Xasync"))
           |  assert(BuildInfo.Main.scalaCompilerPlugins.size == 0)
           |  assert(BuildInfo.Main.dependencies.size == 1)
           |  assert(BuildInfo.Main.dependencies.head.contains("com.lihaoyi:os-lib_"))
           |  assert(BuildInfo.Main.resolvers.size == 3)
           |  assert(BuildInfo.Main.resourceDirs.size == 1)
           |  assert(BuildInfo.Main.customJarsDecls.size == 2)
           |   
           |  assert(BuildInfo.Test.sources.head.endsWith("Test.scala"))
           |  assert(BuildInfo.Test.scalacOptions == Seq("-Xasync"))
           |  assert(BuildInfo.Test.scalaCompilerPlugins.size == 0)
           |  assert(BuildInfo.Test.dependencies.size == 2)
           |  assert(BuildInfo.Test.dependencies.exists(_.contains("com.lihaoyi:os-lib_")))
           |  assert(BuildInfo.Test.dependencies.exists(_.contains("org.scalameta:munit")))
           |  assert(BuildInfo.Test.resolvers.size == 3)
           |  assert(BuildInfo.Test.resourceDirs.size == 1)
           |  assert(BuildInfo.Test.customJarsDecls.size == 2)
           |}
           |""".stripMargin,
      os.rel / "test" / "Test.scala" ->
        """//> using dep org.scalameta::munit::0.7.29
          |
          |class MyTests extends munit.FunSuite {
          |  test("foo") {
          |    assert(2 + 2 == 4)
          |    println("Hello from " + "tests")
          |  }
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      TestUtil.initializeGit(root, "v1.0.0")

      val res =
        os.proc(TestUtil.cli, "--power", extraOptions, ".").call(cwd = root)
      val output = res.out.trim()

      val projectDir = os.list(root / ".scala-build").filter(
        _.baseName.startsWith(root.baseName + "_")
      )
      expect(projectDir.size == 1)
      val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
      expect(os.isFile(buildInfoPath))

      expect(output == "")
    }
  }

  test("BuildInfo should take into account --project-version") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        s"""//> using buildInfo
           |
           |import scala.cli.build.BuildInfo
           |
           |object Main extends App {
           |  assert(BuildInfo.projectVersion == Some("35.0.1"))
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      TestUtil.initializeGit(root, "v1.0.0")

      val res =
        os.proc(
          TestUtil.cli,
          "--power",
          extraOptions,
          ".",
          "--compute-version",
          "git",
          "--project-version",
          "35.0.1"
        ).call(cwd = root)
      val output = res.out.trim()

      val projectDir = os.list(root / ".scala-build").filter(
        _.baseName.startsWith(root.baseName + "_")
      )
      expect(projectDir.size == 1)
      val buildInfoPath = projectDir.head / "src_generated" / "main" / "BuildInfo.scala"
      expect(os.isFile(buildInfoPath))

      expect(output == "")
    }
  }

  // Credentials tests
  test("repository credentials passed to coursier") {
    val testOrg     = "test-org"
    val testName    = "the-messages"
    val testVersion = "0.1.2"
    val user        = "username"
    val password    = "1234"
    val realm       = "Realm"
    val inputs = TestInputs(
      os.rel / "messages" / "Messages.scala" ->
        """package messages
          |
          |object Messages {
          |  def hello(name: String): String =
          |    s"Hello $name"
          |}
          |""".stripMargin,
      os.rel / "hello" / "Hello.scala" ->
        s"""//> using dep "$testOrg::$testName:$testVersion"
           |import messages.Messages
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println(Messages.hello(args.headOption.getOrElse("Unknown")))
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val configFile = {
        val dir = root / "conf"
        os.makeDir.all(dir, if (Properties.isWin) null else "rwx------")
        dir / "config.json"
      }
      val extraEnv = Map(
        "SCALA_CLI_CONFIG" -> configFile.toString
      )
      val repoPath = root / "the-repo"
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "--publish-repo",
        repoPath.toNIO.toUri.toASCIIString,
        "messages",
        "--organization",
        testOrg,
        "--name",
        testName,
        "--project-version",
        testVersion
      )
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit, env = extraEnv)

      TestUtil.serveFilesInHttpServer(repoPath, user, password, realm) { (host, port) =>
        // This codeblock represents test("No repository credentials passed to coursier")
        {
          val resWithNoCreds = os.proc(
            TestUtil.cli,
            "run",
            "--repository",
            s"http://$host:$port",
            "hello",
            "--",
            "TestUser"
          ).call(
            cwd = root,
            env = Map(
              "USER"     -> user,
              "PASSWORD" -> password
            ),
            check = false,
            mergeErrIntoOut = true
          )

          expect(resWithNoCreds.exitCode == 1)
        }

        // This codeblock represents test("Repository credentials passed to coursier - environment variables")
        {
          val resWithEnvVar = os.proc(
            TestUtil.cli,
            "run",
            "--repository",
            s"http://$host:$port",
            "hello",
            "--",
            "TestUser"
          ).call(
            cwd = root,
            env = Map(
              "USER"                 -> user,
              "PASSWORD"             -> password,
              "COURSIER_CREDENTIALS" -> s"$host $user:$password"
            ),
            mergeErrIntoOut = true
          )

          expect(resWithEnvVar.exitCode == 0)
        }

        // This codeblock represents test("Repository credentials passed to coursier - config entry")
        {
          os.write(
            configFile,
            s"""{
               |"repositories.credentials": [
               |{"host":"$host","user":"value:$user","password":"value:$password","matchHost":true}
               |]
               |}""".stripMargin
          )
          val resWithConfig = os.proc(
            TestUtil.cli,
            "run",
            "--repository",
            s"http://$host:$port",
            "hello",
            "--",
            "TestUser"
          ).call(
            cwd = root,
            env = Map(
              "USER"     -> user,
              "PASSWORD" -> password
            ) ++ extraEnv,
            mergeErrIntoOut = true
          )

          expect(resWithConfig.exitCode == 0)
        }

        // This codeblock represents test("Repository credentials passed to coursier - java properties")
        {
          os.write(root / ".scala-jvmopts", s"-Dcoursier.credentials=$host $user:$password\n")

          val resWithProps = os.proc(
            TestUtil.cli,
            "run",
            "--repository",
            s"http://$host:$port",
            "hello",
            "--",
            "TestUser"
          ).call(
            cwd = root,
            env = Map(
              "USER"     -> user,
              "PASSWORD" -> password
            ),
            mergeErrIntoOut = true
          )

          expect(resWithProps.exitCode == 0)
        }
      }
    }
  }

  test("warn about transitive `using file` directive") {
    TestInputs(
      os.rel / "Main.scala" ->
        """//> using file "bar/Bar.scala"
          |//> using file "abc/Abc.scala"
          |object Main extends App {
          | println(Bar(42))
          |}
          |""".stripMargin,
      os.rel / "bar" / "Bar.scala" ->
        """//> using file "xyz/Xyz.scala"
          |//> using file "xyz/NonExistent.scala"
          |case class Bar(x: Int)
          |""".stripMargin,
      os.rel / "abc" / "Abc.scala" ->
        """//> using file "xyz/Xyz.scala"
          |//> using file "xyz/NonExistent.scala"
          |case class Abc(x: Int)
          |""".stripMargin,
      os.rel / "xyz" / "Xyz.scala" ->
        """val xyz = 42
          |""".stripMargin
    ).fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "compile",
        "Main.scala",
        "--suppress-directives-in-multiple-files-warning"
      )
        .call(cwd = root, mergeErrIntoOut = true)

      val output = TestUtil.removeAnsiColors(res.out.trim())

      expect(output.contains(
        """[warn] Chaining the 'using file' directive is not supported, the source won't be included in the build.
          |[warn] //> using file "xyz/Xyz.scala"
          |[warn]                 ^^^^^^^^^^^^^
          |""".stripMargin
      ))

      expect(output.contains(
        """[warn] Chaining the 'using file' directive is not supported, the source won't be included in the build.
          |[warn] //> using file "xyz/NonExistent.scala"
          |[warn]                 ^^^^^^^^^^^^^^^^^^^^^
          |""".stripMargin
      ))

      expect(output.contains(
        """[warn] Chaining the 'using file' directive is not supported, the source won't be included in the build.
          |[warn] //> using file "xyz/Xyz.scala"
          |[warn]                 ^^^^^^^^^^^^^
          |""".stripMargin
      ))

      expect(output.contains(
        """[warn] Chaining the 'using file' directive is not supported, the source won't be included in the build.
          |[warn] //> using file "xyz/NonExistent.scala"
          |[warn]                 ^^^^^^^^^^^^^^^^^^^^^
          |""".stripMargin
      ))
    }
  }

  if (!actualScalaVersion.contains("RC")) {
    val actualAnnouncedScalaVersion = actualScalaVersion match {
      case _
          if actualScalaVersion == Constants.scala3Next &&
          Constants.scala3Next != Constants.scala3NextAnnounced =>
        // if the version isn't public yet, Coursier won't be able to install it
        Constants.scala3NextAnnounced
      case s => s
    }

    test(
      s"offline mode should fail on missing artifacts (with Scala $actualAnnouncedScalaVersion)"
    ) {
      // Kill bloop deamon to test scalac fallback
      os.proc(TestUtil.cli, "--power", "bloop", "exit")
        .call(cwd = os.pwd)

      // ensure extra options use an announced Scala version
      val customExtraOptions: Seq[String] =
        if (
          scalaVersionOpt.isEmpty &&
          Constants.scala3Next != Constants.scala3NextAnnounced
        )
          extraOptions ++ Seq("--scala", actualAnnouncedScalaVersion)
        else if (
          actualScalaVersion == Constants.scala3Next &&
          actualScalaVersion != actualAnnouncedScalaVersion
        )
          extraOptions
            .map {
              case opt if opt == Constants.scala3Next => actualAnnouncedScalaVersion
              case opt                                => opt
            }
        else extraOptions

      val depScalaVersion = actualAnnouncedScalaVersion match {
        case sv if sv.startsWith("2.12") => "2.12"
        case sv if sv.startsWith("2.13") => "2.13"
        case _                           => "3"
      }

      val dep = s"com.lihaoyi:os-lib_$depScalaVersion:0.10.6"
      val inputs = TestInputs(
        os.rel / "NoDeps.scala" ->
          """//> using jvm zulu:11
            |object NoDeps extends App {
            |  println("Hello from NoDeps")
            |}
            |""".stripMargin,
        os.rel / "WithDeps.scala" ->
          s"""//> using jvm zulu:11
             |//> using dep $dep
             |
             |object WithDeps extends App {
             |  println("Hello from WithDeps")
             |}
             |""".stripMargin
      )
      inputs.fromRoot { root =>
        val cachePath = root / ".cache"
        os.makeDir(cachePath)

        val extraEnv = Map("COURSIER_CACHE" -> cachePath.toString)

        val emptyCacheWalkSize = os.walk(cachePath).size

        val noArtifactsRes = os.proc(
          TestUtil.cli,
          "--power",
          "NoDeps.scala",
          customExtraOptions,
          "--offline",
          "--cache",
          cachePath.toString
        )
          .call(cwd = root, check = false, mergeErrIntoOut = true)
        expect(noArtifactsRes.exitCode == 1)

        // Cache unchanged
        expect(emptyCacheWalkSize == os.walk(cachePath).size)

        // Download the artifacts for scala
        os.proc(TestUtil.cs, "install", s"scala:$actualAnnouncedScalaVersion")
          .call(cwd = root, env = extraEnv)
        os.proc(TestUtil.cs, "install", s"scalac:$actualAnnouncedScalaVersion")
          .call(cwd = root, env = extraEnv)
        (if (actualAnnouncedScalaVersion.startsWith("3")) Some("scala3-sbt-bridge")
         else if (
           actualAnnouncedScalaVersion.startsWith("2.13.") &&
           actualAnnouncedScalaVersion.coursierVersion >= "2.13.12".coursierVersion
         )
           Some("scala2-sbt-bridge")
         else None)
          .foreach { bridgeArtifactName =>
            os.proc(
              TestUtil.cs,
              "fetch",
              s"org.scala-lang:$bridgeArtifactName:$actualAnnouncedScalaVersion"
            )
              .call(cwd = root, env = extraEnv)
          }

        // Download JVM that won't suit Bloop, also no Bloop artifacts are present
        os.proc(TestUtil.cs, "java-home", "--jvm", "zulu:11")
          .call(cwd = root, env = extraEnv)

        val scalaJvmCacheWalkSize = os.walk(cachePath).size

        val scalaAndJvmRes = os.proc(
          TestUtil.cli,
          "--power",
          "NoDeps.scala",
          customExtraOptions,
          "--offline",
          "--cache",
          cachePath.toString,
          "-v",
          "-v"
        )
          .call(cwd = root, mergeErrIntoOut = true)
        expect(scalaAndJvmRes.exitCode == 0)
        expect(scalaAndJvmRes.out.trim().contains(
          "Offline mode is ON and Bloop could not be fetched from the local cache, using scalac as fallback"
        ))
        expect(scalaAndJvmRes.out.trim().contains("Hello from NoDeps"))

        // Cache unchanged
        expect(scalaJvmCacheWalkSize == os.walk(cachePath).size)

        // Missing dependencies
        for {
          (cliOption, extraEnvMode) <- Seq(
            "--offline"               -> Map.empty[String, String],
            "-Dcoursier.mode=offline" -> Map.empty[String, String],
            ""                        -> Map("COURSIER_MODE" -> "offline")
          )
        } {
          val missingDepsRes = os.proc(
            TestUtil.cli,
            "--power",
            cliOption,
            "WithDeps.scala",
            customExtraOptions,
            "--cache",
            cachePath.toString
          )
            .call(cwd = root, check = false, mergeErrIntoOut = true, env = extraEnvMode)
          expect(missingDepsRes.exitCode == 1)
          expect(missingDepsRes.out.trim().contains("Error downloading com.lihaoyi:os-lib"))

          // Cache unchanged
          expect(scalaJvmCacheWalkSize == os.walk(cachePath).size)
        }

        // Download dependencies
        os.proc(TestUtil.cs, "fetch", dep)
          .call(cwd = root, env = extraEnv)

        val withDependencyCacheWalkSize = os.walk(cachePath).size

        val depsRes = os.proc(
          TestUtil.cli,
          "--power",
          "WithDeps.scala",
          customExtraOptions,
          "--offline",
          "--cache",
          cachePath.toString,
          "-v",
          "-v"
        )
          .call(cwd = root, mergeErrIntoOut = true)
        expect(depsRes.exitCode == 0)
        expect(
          depsRes.out.trim().contains(
            "Offline mode is ON and Bloop could not be fetched from the local cache, using scalac as fallback"
          )
        )
        expect(depsRes.out.trim().contains("Hello from WithDeps"))

        // Cache changed
        expect(withDependencyCacheWalkSize == os.walk(cachePath).size)
      }
    }
  }

  test("JVM id is printed with compilation info correctly") {
    val msg   = "Hello"
    val input = "jvm.sc"
    TestInputs(os.rel / input ->
      s"""//> using jvm 11
         |println("$msg")
         |""".stripMargin).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, input).call(cwd = root, stderr = os.Pipe)
      expect(res.out.trim() == msg)
      expect(res.err.trim().contains("JVM (11)"))
    }
  }

  def getCoursierCacheRelPath: os.RelPath =
    if (Properties.isWin) os.rel / "Coursier" / "Cache"
    else if (Properties.isMac) os.rel / "Library" / "Caches" / "Coursier"
    else os.rel / ".cache" / "coursier"

  if (TestUtil.isJvmCli) // can't reproduce on native image launchers
    test("doesn't fail on invalid user.home") {
      val customCall =
        Seq("java", "-Xmx512m", "-Xms128m", "-Duser.home=?", "-jar", TestUtil.cliPath)
      val msg   = "Hello"
      val input = "script.sc"
      TestInputs(os.rel / input -> s"println(\"$msg\")")
        .fromRoot { root =>
          val res = os.proc(customCall, "run", extraOptions, "--server=false", input)
            .call(
              cwd = root,
              stderr = os.Pipe
            )
          expect(res.out.trim() == msg)
        }
    }

  test("toolkit default") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """//> using toolkit default
          |//> using toolkit typelevel:default
          |//> using toolkit org.typelevel:default
          |
          |import cats.effect.IOApp
          |import cats.effect.IO
          |
          |object Hello extends IOApp.Simple {
          |  def run =  IO.println(os.pwd)
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val result =
        os.proc(TestUtil.cli, extraOptions, ".").call(cwd = root, stderr = os.Pipe, check = false)
      if (actualScalaVersion.startsWith("2.12")) {
        expect(result.exitCode == 1)
        expect(result.err.trim().contains("Toolkits do not support Scala 2.12"))
      }
      else {
        expect(result.exitCode == 0)
        expect(result.out.trim() == root.toString)
      }
    }
  }

  test("warning about using toolkit latest in options should be reported") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """object Main {
          |  def main(args: Array[String]): Unit = {
          |    println(os.pwd)
          |  }
          |}
          |""".stripMargin
    )

    inputs.fromRoot { root =>
      val resLatest = os.proc(TestUtil.cli, extraOptions, ".", "--toolkit", "latest").call(
        cwd = root,
        mergeErrIntoOut = true
      )
      val warningText = "Using 'latest' for toolkit is deprecated"
      expect(resLatest.out.text().contains(warningText))
      val warningCount = resLatest.out.text().sliding(warningText.length).count(_ == warningText)
      expect(warningCount == 1)
    }
  }

  test("running a .scala file several times doesn't produce Bloop errors") {
    val msg   = "Hello"
    val input = "Main.scala"
    TestInputs(
      os.rel / input ->
        s"""object Main {
           |  def main(args: Array[String]): Unit = {
           |    println("$msg")
           |  }
           |}
           |""".stripMargin
    ).fromRoot { root =>
      // ensure the test will be run on a fresh Bloop instance
      os.proc(TestUtil.cli, "bloop", "exit", "--power").call(cwd = root)
      (0 to 2).foreach { _ =>
        val res = os.proc(TestUtil.cli, "run", input, extraOptions)
          .call(cwd = root, stderr = os.Pipe)
        expect(res.out.trim() == msg)
        expect(!res.err.trim().toLowerCase.contains("error"))
      }
    }
  }

  test(s"warn about invalid values present in JAVA_OPTS") {
    val expectedOutput = "Hello"
    TestInputs(os.rel / "example.sc" -> s"println(\"$expectedOutput\")")
      .fromRoot { root =>
        val invalidOpt = "--invalid"
        val validOpt   = "-Dfoo=bar"
        val res = os.proc(TestUtil.cli, "run", "example.sc", "--server=false", extraOptions)
          .call(cwd = root, env = Map("JAVA_OPTS" -> s"$invalidOpt $validOpt"), stderr = os.Pipe)
        val errOutput = res.err.trim()
        expect(errOutput.contains(
          s"Only java properties are supported in JAVA_OPTS"
        ))
        expect(errOutput.contains(s"Other options are ignored: $invalidOpt"))
        expect(!errOutput.contains(validOpt))
        expect(res.out.trim() == expectedOutput)
      }
  }

  test(s"warn about invalid values present in .scala-jvmopts") {
    val expectedOutput = "Hello"
    val invalidOpt     = "--invalid"
    val validOpt       = "-Dfoo=bar"
    TestInputs(
      os.rel / "example.sc" -> s"println(\"$expectedOutput\")",
      os.rel / ".scala-jvmopts" ->
        s"""$invalidOpt
           |$validOpt
           |""".stripMargin
    )
      .fromRoot { root =>
        val res = os.proc(TestUtil.cli, "run", "example.sc", extraOptions)
          .call(cwd = root, stderr = os.Pipe)
        val errOutput = res.err.trim()
        expect(errOutput.contains(s"Only java properties are supported in .scala-jvmopts file"))
        expect(errOutput.contains(s"Other options are ignored: $invalidOpt"))
        expect(!errOutput.contains(validOpt))
        expect(res.out.trim() == expectedOutput)
      }
  }

  {
    val expectedMessage = "Hello"
    for {
      (actualInputPath, inputPathToCall, inputs) <- {
        val scalaInputPath  = os.rel / "Main.scala"
        val scriptInputPath = os.rel / "script.sc"
        val scalaInputs = TestInputs(
          scalaInputPath -> s"""object Main extends App { println("$expectedMessage") }"""
        )
        val scriptInputs = TestInputs(scriptInputPath -> s"""println("$expectedMessage")""")
        Seq(
          (scalaInputPath, ".", scalaInputs),
          (scalaInputPath, scalaInputPath.toString, scalaInputs),
          (scriptInputPath, ".", scriptInputs),
          (scriptInputPath, scriptInputPath.toString, scriptInputs)
        )
      }
      inputExtension = "." + actualInputPath.last.split('.').last
    }
      test(
        s"prioritise main class in a $inputExtension file passed as $inputPathToCall over main classes in dependencies on the classpath"
      ) {
        inputs.fromRoot { root =>
          val localCache        = root / "local-cache"
          val dependencyVersion = "42.7.4"
          val csRes = os.proc(
            TestUtil.cs,
            "fetch",
            "--cache",
            localCache,
            s"org.postgresql:postgresql:$dependencyVersion"
          )
            .call(cwd = root)
          val dependencyJar = csRes.out.trim().linesIterator.toSeq.head

          // pass classpath via -cp
          val res =
            os.proc(TestUtil.cli, "run", inputPathToCall, extraOptions, "-cp", dependencyJar)
              .call(cwd = root)
          expect(res.out.trim() == expectedMessage)

          // pass classpath via args file
          val argsFileName = "args.txt"
          os.write(root / argsFileName, s"-cp $dependencyJar")
          val res2 = os.proc(TestUtil.cli, "run", inputPathToCall, extraOptions, s"@$argsFileName")
            .call(cwd = root)
          expect(res2.out.trim() == expectedMessage)
        }
      }
  }
}
