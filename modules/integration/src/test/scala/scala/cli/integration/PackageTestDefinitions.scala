package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{File, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util
import java.util.zip.ZipFile

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.jdk.CollectionConverters.*
import scala.util.{Properties, Using}

abstract class PackageTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  def maybeUseBash(cmd: os.Shellable*)(cwd: os.Path = null): os.CommandResult = {
    val res = os.proc(cmd*).call(cwd = cwd, check = false)
    if (Properties.isLinux && res.exitCode == 127)
      // /bin/sh seems to have issues with '%' signs in PATH, that coursier can leave
      // in the JVM path entry (https://unix.stackexchange.com/questions/126955/percent-in-path-environment-variable)
      os.proc((("/bin/bash": os.Shellable) +: cmd)*).call(cwd = cwd)
    else {
      expect(res.exitCode == 0)
      res
    }
  }

  test("simple script") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
    )
    val launcherName = {
      val ext = if (Properties.isWin) ".bat" else ""
      fileName.stripSuffix(".sc") + ext
    }
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, fileName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / launcherName

      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = maybeUseBash(launcher)(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("current directory as default input") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""val msg = "$message"
           |println(msg)
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "simple.bat" else "simple"
      val launcher   = root / outputName

      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = maybeUseBash(launcher.toString)(cwd = root).out.trim()
      expect(output == message)
    }
  }
  test("resource directory for coursier bootstrap launcher") {
    val fileName = "hello.sc"
    val message  = "1,2,3"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|//> using resourceDir "."
            |import scala.io.Source
            |
            |val inputs = Source.fromResource("input").getLines.toSeq
            |println(inputs.mkString)
            |""".stripMargin,
      os.rel / "input" -> message
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "hello.bat" else "hello"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  test("resource directory for library package") {
    val fileName     = "MyLibrary.scala"
    val outputLib    = "my-library.jar"
    val resourceFile = "input"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|//> using resourceDir "."
            |
            |class MyLibrary {
            |  def message = "Hello"
            |}
            |""".stripMargin,
      os.rel / resourceFile -> "1,2,3"
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        ".",
        "-o",
        outputLib,
        "--library"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val zf    = new ZipFile((root / outputLib).toIO)
      val entry = zf.getEntry(resourceFile)
      expect(entry != null)
    }
  }

  test("Zip with Scala Script containing resource directive") {
    val inputs = TestInputs(
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
    inputs.asZip { (root, zipPath) =>
      val message = "1,2"

      os.proc(TestUtil.cli, "--power", "package", zipPath, extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "hello.bat" else "hello"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  def simpleJsTest(): Unit = {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalajs.js
           |val console = js.Dynamic.global.console
           |val msg = "$message"
           |console.log(msg)
           |""".stripMargin
    )
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, fileName, "--js").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))

      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      val output   = os.proc(nodePath, launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  def sourceMapJsTest(): Unit = {
    val fileName = "Hello.scala"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""import scala.scalajs.js
           |
           |object Hello extends App {
           |  println("Hello World")
           |}
           |""".stripMargin
    )
    val destName = fileName.stripSuffix(".scala") + ".js"
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        fileName,
        "--js",
        "--js-emit-source-maps"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val expectedSourceMapsPath = root / s"$destName.map"
      val expectedHelloJsPath    = root / destName
      expect(os.isFile(expectedHelloJsPath))
      expect(os.isFile(expectedSourceMapsPath))

      val jsContent        = os.read(expectedHelloJsPath)
      val sourceMappingURL = jsContent.split(System.lineSeparator()).toList.lastOption
      expect(sourceMappingURL.nonEmpty)
      expect(sourceMappingURL.get == s"//# sourceMappingURL=$destName.map")
    }
  }

  def multiModulesJsTest(): Unit = {
    val fileName = "Hello.scala"
    val message  = "Hello World from JS"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|//> using jsModuleKind "es"
            |//> using jsModuleSplitStyleStr "smallestmodules"
            |
            |case class Foo(bar: String)
            |
            |object Hello extends App {
            |  println(Foo("$message").bar)
            |}
            |""".stripMargin
    )
    val destDir = fileName.stripSuffix(".scala")
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        fileName,
        "--js",
        "-o",
        destDir
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destDir / "main.js"
      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      os.write(root / "package.json", "{\n\n  \"type\": \"module\"\n\n}") // enable es module
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }
  def smallModulesJsTest(jvm: Boolean): Unit = {
    val fileName = "Hello.scala"
    val message  = "Hello World from JS"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|//> using jsModuleKind "es"
            |//> using jsModuleSplitStyleStr "smallmodulesfor"
            |//> using jsSmallModuleForPackage "test"
            |
            |package test
            |
            |case class Foo(bar: String)
            |
            |object Hello extends App {
            |  println(Foo("$message").bar)
            |}
            |""".stripMargin
    )
    val destDir = fileName.stripSuffix(".scala")
    inputs.fromRoot { root =>
      val extraArgs = if (jvm) Seq("--js-cli-on-jvm") else Nil
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        fileName,
        "--js",
        "-o",
        destDir,
        extraArgs
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destDir / "main.js"
      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      os.write(root / "package.json", "{\n\n  \"type\": \"module\"\n\n}") // enable es module
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  def jsHeaderTest(): Unit = {
    val fileName        = "Hello.scala"
    val jsHeader        = "#!/usr/bin/env node"
    val jsHeaderNewLine = s"$jsHeader\\n"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|//> using jsHeader "$jsHeaderNewLine"
            |//> using jsMode "release"
            |
            |object Hello extends App {
            |  println("Hello")
            |}
            |""".stripMargin
    )
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        fileName,
        "--js",
        "-o",
        destName
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher        = root / destName
      val launcherContent = os.read(launcher)
      expect(launcherContent.startsWith(jsHeader))
    }
  }

  def jsWithoutMainTest(): Unit = {
    val fileName = "Hello.scala"
    val msg      = "Hello World"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""|import scala.scalajs.js.annotation._
            |
            |@JSExportTopLevel("Hello")
            |object Hello {
            |  @JSExport
            |  def helloWorld: String = "$msg"
            |}
            |""".stripMargin,
      os.rel / "runHello.js" ->
        s"""const { Hello } = require('./Hello.js');
           |console.log(Hello.helloWorld);
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        fileName,
        "--js",
        "--js-module-kind",
        "common"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val runHelloWorld = root / "runHello.js"
      val nodePath      = TestUtil.fromPath("node").getOrElse("node")
      val output        = os.proc(nodePath, runHelloWorld.toString).call(cwd = root).out.trim()
      expect(output == msg)
    }
  }

  if (!TestUtil.isNativeCli || !Properties.isWin) {
    test("simple JS") {
      simpleJsTest()
    }
    test("source maps js") {
      sourceMapJsTest()
    }
    test("multi modules js") {
      multiModulesJsTest()
    }
    test("small modules js with native scalajs-cli") {
      smallModulesJsTest(jvm = false)
    }
    test("small modules js with jvm scalajs-cli") {
      smallModulesJsTest(jvm = true)
    }
    test("js header in release mode") {
      jsHeaderTest()
    }
    test("js without main") {
      jsWithoutMainTest()
    }
  }

  def simpleNativeTest(): Unit = {
    val fileName   = "simple.sc"
    val message    = "Hello"
    val platformNl = if (Properties.isWin) "\\r\\n" else "\\n"
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
    val destName = {
      val ext = if (Properties.isWin) ".exe" else ""
      fileName.stripSuffix(".sc") + ext
    }
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, fileName, "--native").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.trim()
      expect(output == message)
    }
  }

  def libraryNativeTest(
    shared: Boolean = false,
    commandLineShared: Option[Boolean] = None
  ): Unit = {
    val fileName              = "simple.sc"
    val directiveNativeTarget = if (shared) "dynamic" else "static"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""
           |//> using platform scala-native
           |//> using nativeTarget $directiveNativeTarget
           |import scala.scalanative.unsafe._
           |object myLib{
           |  @exported
           |  def addLongs(l: Long, r: Long): Long = l + r
           |  @exported("mylib_addInts")
           |  def addInts(l: Int, r: Int): Int = l + r
           |}""".stripMargin
    )
    val destName = {
      val ext =
        if (!shared && !commandLineShared.getOrElse(false))
          if (Properties.isWin) ".lib" else ".a"
        else if (Properties.isWin) ".dll"
        else if (Properties.isMac) ".dylib"
        else ".so"
      fileName.stripSuffix(".sc") + ext
    }

    val nativeTargetOpts = commandLineShared match {
      case Some(true)  => Seq("--native-target", "dynamic")
      case Some(false) => Seq("--native-target", "static")
      case None        => Seq.empty
    }

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, nativeTargetOpts, fileName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val library = root / destName
      expect(os.isFile(library))
    }
  }

  if (!Properties.isWin && actualScalaVersion.startsWith("2.13")) {
    test("simple native") {
      simpleNativeTest()
    }
    test("dynamic library native") {
      libraryNativeTest(shared = true)
    }

    test("dynamic library native override from command line") {
      libraryNativeTest(shared = false, commandLineShared = Some(true))
    }

    // To produce a static library, `LLVM_BIN` environment variable needs to be
    // present (for `llvm-ar` utility)
    if (sys.env.contains("LLVM_BIN"))
      test("shared library native") {
        libraryNativeTest(shared = false)
      }

  }

  test("assembly") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""//> using dep "org.typelevel::cats-kernel:2.6.1"
           |import cats.kernel._
           |val m = Monoid.instance[String]("", (a, b) => a + b)
           |val msgStuff = m.combineAll(List("$message", "", ""))
           |println(msgStuff)
           |""".stripMargin
    )
    val launcherName = fileName.stripSuffix(".sc") + ".jar"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, "--assembly", fileName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / launcherName
      expect(os.isFile(launcher))

      var zf: ZipFile = null
      try {
        zf = new ZipFile(launcher.toIO)
        expect(zf.getEntry("cats/kernel/Monoid.class") != null)
      }
      finally if (zf != null) zf.close()

      val runnableLauncher =
        if (Properties.isWin) {
          val bat = root / "assembly.bat"
          os.copy(launcher, bat)
          bat
        }
        else {
          expect(Files.isExecutable(launcher.toNIO))
          launcher
        }
      val runnableLauncherSize = os.size(runnableLauncher)

      val output                  = maybeUseBash(runnableLauncher.toString)(cwd = root).out.trim()
      val maxRunnableLauncherSize = 1024 * 1024 * 12 // should be smaller than 12MB
      expect(output == message)
      expect(runnableLauncherSize < maxRunnableLauncherSize)
    }
  }

  test("assembly no preamble") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""package hello
           |
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("Hello from " + "assembly")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--assembly",
        "-o",
        "hello",
        "--preamble=false",
        "."
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / "hello"
      expect(os.isFile(launcher))

      val preambleStart = "#".getBytes(StandardCharsets.UTF_8)
      val contentStart  = os.read.bytes(launcher).take(preambleStart.length)
      expect(!util.Arrays.equals(contentStart, preambleStart))

      val output = os.proc("java", "-cp", launcher, "hello.Hello")
        .call(cwd = root).out.trim()
      expect(output == "Hello from assembly")
    }
  }

  test("assembly no preamble nor main class") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""package hello
           |
           |object Hello {
           |  def message: String =
           |    "Hello from " + "assembly"
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--assembly",
        "-o",
        "hello.jar",
        "--preamble=false",
        "."
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / "hello.jar"
      expect(os.isFile(launcher))

      Using.resource(new ZipFile(launcher.toIO)) { zf =>
        val entries = zf.entries()
          .asScala
          .iterator
          .map(_.getName)
          .filter(_.startsWith("hello/"))
          .toVector
        expect(entries.contains("hello/Hello.class"))
      }
    }
  }

  test("assembly provided") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""package hello
           |
           |object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("Hello from Scala " + scala.util.Properties.versionNumberString)
           |}
           |""".stripMargin
    )
    val providedModule =
      if (actualScalaVersion.startsWith("2.")) "org.scala-lang:scala-library"
      else "org.scala-lang:scala3-library_3"
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--assembly",
        "-o",
        "hello",
        "--provided",
        providedModule,
        "."
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / "hello"
      expect(os.isFile(launcher))

      var zf: ZipFile = null
      val entries =
        try {
          zf = new ZipFile(launcher.toIO)
          expect(zf.getEntry("hello/Hello.class") != null)
          expect(zf.getEntry("scala/Function.class") == null) // no scala-library
          expect(zf.getEntry("scala/Tuple.class") == null)    // no scala3-library

          zf.entries().asScala.map(_.getName).toVector.sorted
        }
        finally if (zf != null) zf.close()

      val noMetaEntries = entries.filter(!_.startsWith("META-INF/"))
      expect(noMetaEntries.nonEmpty)
      expect(noMetaEntries.forall(_.startsWith("hello/")))

      val scalaLibCp =
        os.proc(TestUtil.cs, "fetch", "--classpath", s"$providedModule:$actualScalaVersion")
          .call(cwd = root).out.trim()
      val output =
        os.proc("java", "-cp", s"$launcher${File.pathSeparator}$scalaLibCp", "hello.Hello")
          .call(cwd = root).out.trim()
      val expectedScalaVerInOutput =
        if (actualScalaVersion.startsWith("2.")) actualScalaVersion
        else {
          val scalaLibJarName = scalaLibCp.split(File.pathSeparator)
            .map(_.split("[\\\\/]+").last).find(_.startsWith("scala-library-"))
            .getOrElse {
              sys.error(s"scala-library not found in provided class path $scalaLibCp")
            }
          scalaLibJarName
            .stripPrefix("scala-library-")
            .stripSuffix(".jar")
        }
      expect(output == "Hello from Scala " + expectedScalaVerInOutput)
    }
  }

  test("ignore test scope") {
    val inputs = TestInputs(
      os.rel / "Main.scala" ->
        """|object Main {
           |  def main(args: Array[String]): Unit = {
           |    println("Hello World")
           |  }
           |}""".stripMargin,
      os.rel / "Tests.test.scala" ->
        """|import utest._ // compilation error, not included test library
           |
           |object Tests extends TestSuite {
           |  val tests = Tests {
           |    test("message") {
           |      assert(1 == 1)
           |    }
           |  }
           |}""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "Main.bat" else "Main"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.trim()
      expect(output == "Hello World")
    }
  }

  private def readEntry(zf: ZipFile, name: String): Array[Byte] = {
    val ent             = zf.getEntry(name)
    var is: InputStream = null
    try {
      is = zf.getInputStream(ent)
      is.readAllBytes()
    }
    finally
      if (is != null)
        is.close()
  }

  private val simpleInputWithScalaAndSc = TestInputs(
    os.rel / "lib" / "Messages.scala" ->
      """package lib
        |
        |object Messages {
        |  def msg = "Hello"
        |}
        |""".stripMargin,
    os.rel / "simple.sc" ->
      """val msg = lib.Messages.msg
        |println(msg)
        |""".stripMargin
  )
  test("source JAR") {
    val dest = os.rel / "sources.jar"
    simpleInputWithScalaAndSc.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, ".", "-o", dest, "--source").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isFile(root / dest))

      val zf                 = new ZipFile((root / dest).toIO)
      val genSourceEntryName = "META-INF/generated/simple.scala"
      val expectedEntries = Set(
        "lib/Messages.scala",
        genSourceEntryName,
        "simple.sc"
      )
      val entries = zf.entries().asScala.iterator.map(_.getName).toSet
      expect(entries == expectedEntries)

      for ((relPath, expectedStrContent) <- simpleInputWithScalaAndSc.files) {
        val content    = readEntry(zf, relPath.toString)
        val strContent = new String(content, StandardCharsets.UTF_8)
        expect(strContent == expectedStrContent)
      }

      val genContent    = readEntry(zf, genSourceEntryName)
      val genContentStr = new String(genContent, StandardCharsets.UTF_8)
      if (actualScalaVersion.startsWith("2."))
        expect(genContentStr.contains("object simple"))
      else
        expect(genContentStr.contains("class simple$_"))
    }
  }

  test("doc JAR") {
    val dest = os.rel / "doc.jar"
    simpleInputWithScalaAndSc.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", extraOptions, ".", "-o", dest, "--doc").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isFile(root / dest))
      val zf = new ZipFile((root / dest).toIO)
      val expectedEntries =
        if (actualScalaVersion.startsWith("2."))
          Seq(
            "index.html",
            "lib/Messages$.html",
            "simple$.html"
          )
        else
          Seq(
            "index.html",
            "inkuire-db.json",
            "_empty_/simple$_.html",
            "lib/Messages$.html"
          )
      val entries = zf.entries().asScala.iterator.map(_.getName).toSet

      expect(expectedEntries.forall(e => entries.contains(e)))
    }
  }

  def packageProc(extraArgs: os.Shellable*) =
    os.proc(
      TestUtil.cli,
      "--power",
      "package",
      extraOptions,
      ".",
      extraArgs,
      "--native-image",
      "--",
      "--no-fallback"
    )

  test("native image") {
    val message = "Hello from native-image"
    val dest    = "hello"
    val actualDest =
      if (Properties.isWin) "hello.exe"
      else "hello"
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("$message")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      packageProc("-o", dest).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isFile(root / actualDest))

      // FIXME Check that dest is indeed a binary?

      val res    = os.proc(root / actualDest).call(cwd = root)
      val output = res.out.trim()
      expect(output == message)
    }
  }

  for (jvmVersion <- Seq(8, 11, 17, 21))
    test(s"native image with JVM $jvmVersion") {
      val message = "Hello from native-image"
      val dest    = "hello"
      val actualDest =
        if (Properties.isWin) "hello.exe"
        else "hello"
      val inputs = TestInputs(
        os.rel / "Hello.scala" ->
          s"""object Hello {
             |  def main(args: Array[String]): Unit =
             |    println("$message")
             |}
             |""".stripMargin
      )

      val extraEnv: Map[String, String] = if (jvmVersion == 21)
        Map("USE_NATIVE_IMAGE_JAVA_PLATFORM_MODULE_SYSTEM" -> "true")
      else Map.empty

      inputs.fromRoot { root =>
        packageProc("--jvm", jvmVersion, "-o", dest).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          env = extraEnv
        )

        expect(os.isFile(root / actualDest))

        val res    = os.proc(root / actualDest).call(cwd = root)
        val output = res.out.trim()
        expect(output == message)
      }
    }

  test(s"native image with --jvm 21 and --graalvm-java-version 17") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("nope")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = packageProc("--jvm", 21, "--graalvm-java-version", 17)
        .call(
          cwd = root,
          stderr = os.Pipe,
          check = false
        )

      expect(res.exitCode == 1)

      val errOutput = res.err.text().trim()
        .linesIterator
        .dropWhile(!_.contains("error"))
        .mkString(System.lineSeparator())
        .replace("Custom(/usr/libexec/java_home -v)", "CommandLine(--jvm)")

      assertNoDiff(
        errOutput,
        """[error]  Cannot build a native image with a JVM older than the one used for compilation.
          |Specified Versions:
          | - compilation JVM: 21, taken from CommandLine(--jvm)
          | - graalVM JVM specified: 17
          |""".stripMargin
      )
    }
  }

  test(s"native image with --graalvm-vm-version 99") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""object Hello {
           |  def main(args: Array[String]): Unit =
           |    println("nope")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = packageProc("--jvm", 17, "--graalvm-version", 99)
        .call(
          cwd = root,
          stderr = os.Pipe,
          check = false
        )

      expect(res.exitCode == 1)

      val errOutput = res.err.text().trim()
        .linesIterator
        .dropWhile(!_.contains("error"))
        .filterNot(_.startsWith(" - "))
        .mkString(System.lineSeparator())
      assertNoDiff(
        errOutput,
        """[error]  Couldn't fetch a correct GraalVM JVM: No GraalVM found with version 99
          |Available versions for the deduced JVM 17:
          |Use '--graalvm-version' to force a different GraalVM version.
          |Use '--graalvm-java-version' to force a different JVM version.
          |Or Use '--graalvm-jvm-id' to specify both, e.g. '--graalvm-jvm-id graalvm-java17:22.0.0'.
          |""".stripMargin,
        clue = res.err.text().trim()
      )
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
        "--power",
        "package",
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

  test("pass java and javac options") {
    val fileName           = "Hello.scala"
    val destFile           = if (Properties.isWin) "hello.bat" else "hello"
    val (fooProp, barProp) = ("abc", "xyz")
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""object Hello {
           |  def main(args: Array[String]): Unit =
           |    println(s"$${sys.props("foo")}$${sys.props("bar")}")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        fileName,
        "-o",
        destFile,
        "--java-prop",
        s"foo=$fooProp",
        "--java-opt",
        s"-Dbar=$barProp",
        "--javac-option",
        "-source",
        "--javac-option",
        "1.8",
        "--javac-option",
        "-target",
        "--javac-option",
        "1.8",
        "-f"
      ).call(cwd = root)
      val output = os.proc(root / destFile).call(cwd = root).out.trim()
      expect(output == s"$fooProp$barProp")
    }
  }

  test("ensure directories are created recursively when packaging a jar") {
    TestInputs(
      os.rel / "Simple.scala" -> s"""object Simple extends App { println() }"""
    ).fromRoot { (root: os.Path) =>
      val jarPath =
        os.rel / "out" / "inner-out" / "Simple.jar" // the `out` directory doesn't exist and should be created
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        ".",
        "--library",
        "-o",
        jarPath,
        extraOptions
      ).call(cwd = root)
    }
  }

  def javaOptionsDockerTest(): Unit = {
    val fileName           = "Hello.scala"
    val imageName          = "hello"
    val (fooProp, barProp) = ("abc", "xyz")
    val inputs = TestInputs(
      os.rel / fileName ->
        s"""object Hello {
           |  def main(args: Array[String]): Unit =
           |    println(s"$${sys.props("foo")}$${sys.props("bar")}")
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        fileName,
        "--docker",
        "--docker-image-repository",
        imageName,
        "--java-prop",
        s"foo=$fooProp",
        "--java-opt",
        s"-Dbar=$barProp",
        "-f"
      ).call(cwd = root)
      val output = os.proc("docker", "run", imageName).call(cwd = root).out.trim()
      expect(output == s"$fooProp$barProp")
    }
  }

  if (Properties.isLinux)
    test("pass java options to docker") {
      javaOptionsDockerTest()
    }

  test("default values in help") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--power", "package", extraOptions, "--help").call(cwd = root)
      val lines = removeAnsiColors(res.out.trim()).linesIterator.toVector

      val graalVmVersionHelp     = lines.find(_.contains("--graalvm-version")).getOrElse("")
      val graalVmJavaVersionHelp = lines.find(_.contains("--graalvm-java-version")).getOrElse("")

      expect(graalVmVersionHelp.contains(s"(${Constants.defaultGraalVMVersion} by default)"))
      expect(
        graalVmJavaVersionHelp.contains(s"(${Constants.defaultGraalVMJavaVersion} by default)")
      )
    }
  }

  test("scalapy") {

    def maybeScalapyPrefix =
      if (actualScalaVersion.startsWith("2.13.")) ""
      else "import me.shadaj.scalapy.py" + System.lineSeparator()

    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""$maybeScalapyPrefix
           |object Hello {
           |  def main(args: Array[String]): Unit = {
           |    py.Dynamic.global.print("Hello from Python", flush = true)
           |  }
           |}
           |""".stripMargin
    )

    val dest =
      if (Properties.isWin) "hello.bat"
      else "hello"

    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "package", "--python", ".", "-o", dest, extraOptions)
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val launcher = root / dest
      val res      = os.proc(launcher).call(cwd = root)
      val output   = res.out.trim()
      expect(output == "Hello from Python")
    }
  }

  test("fat jar") {
    val inputs = TestInputs(
      os.rel / "OsLibFatJar.scala" -> s"""//> using dep "com.lihaoyi::os-lib:0.9.0" """,
      os.rel / "Hello.scala" ->
        s"""object Main extends App {
           |  println(os.pwd)
           |}
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val fatJarPath = root / "OsLibFatJar.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        "OsLibFatJar.scala",
        "-o",
        fatJarPath,
        "--assembly",
        "--preamble=false",
        extraOptions
      ).call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val outputName = if (Properties.isWin) "hello.bat" else "hello"
      val launcher   = root / outputName
      val packageCmds = Seq[os.Shellable](
        TestUtil.cli,
        "--power",
        "package",
        "Hello.scala",
        "-M",
        "Main",
        "--jar",
        fatJarPath,
        "-o",
        launcher,
        extraOptions
      )

      // bootstrap
      os.proc(packageCmds).call(cwd = root).out.trim()
      val output = maybeUseBash(launcher.toString)(cwd = root).out.trim()
      expect(output == root.toString)

      // assembly
      os.proc(packageCmds, "--assembly", "-f").call(cwd = root).out.trim()
      val outputAssembly = maybeUseBash(launcher.toString)(cwd = root).out.trim()
      expect(outputAssembly == root.toString)
    }
  }

  if (actualScalaVersion.startsWith("2")) {
    test("resolution is kept for assemblies with provided spark deps (packaging.provided)") {
      val msg       = "Hello"
      val inputPath = os.rel / "Hello.scala"
      TestInputs(
        inputPath ->
          s"""//> using lib org.apache.spark::spark-sql:3.3.2
             |//> using lib org.apache.spark::spark-hive:3.3.2
             |//> using lib org.apache.spark::spark-sql-kafka-0-10:3.3.2
             |//> using packaging.packageType assembly
             |//> using packaging.provided org.apache.spark::spark-sql
             |//> using packaging.provided org.apache.spark::spark-hive
             |
             |object Main extends App {
             |  println("$msg")
             |}
             |""".stripMargin
      ).fromRoot { root =>
        val outputJarPath = root / "Hello.jar"
        val res = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          inputPath,
          "-o",
          outputJarPath,
          extraOptions
        ).call(cwd = root, stderr = os.Pipe)
        expect(os.isFile(outputJarPath))
        expect(res.err.trim().contains(s"Wrote $outputJarPath"))
      }
    }

    test(
      "resolution is kept for assemblies with provided spark deps (packaging.packageType spark)"
    ) {
      val msg       = "Hello"
      val inputPath = os.rel / "Hello.scala"
      TestInputs(
        inputPath ->
          s"""//> using lib org.apache.spark::spark-sql:3.3.2
             |//> using lib org.apache.spark::spark-hive:3.3.2
             |//> using lib org.apache.spark::spark-sql-kafka-0-10:3.3.2
             |//> using packaging.packageType spark
             |
             |object Main extends App {
             |  println("$msg")
             |}
             |""".stripMargin
      ).fromRoot { root =>
        val outputJarPath = root / "Hello.jar"
        val res = os.proc(
          TestUtil.cli,
          "--power",
          "package",
          inputPath,
          "-o",
          outputJarPath,
          extraOptions
        ).call(cwd = root, stderr = os.Pipe)
        expect(os.isFile(outputJarPath))
        expect(res.err.trim().contains(s"Wrote $outputJarPath"))
      }
    }
  }

  test("pass resource dir with command line option") {
    val child       = "<name>exampleResource</name>"
    val mainClass   = "Hello"
    val xmlFileName = "example.xml"
    val resourceDir = "resources"
    TestInputs(
      os.rel / resourceDir / xmlFileName -> s"<example>$child</example>",
      os.rel / s"$mainClass.scala" ->
        s"""//> using dep org.scala-lang.modules::scala-xml:2.2.0
           |object $mainClass {
           |  def main(args: Array[String]): Unit = {
           |    val xml = scala.xml.XML.load(getClass.getResourceAsStream("$xmlFileName"))
           |    xml.child.foreach(println)
           |  }
           |}
           |""".stripMargin
    ).fromRoot { root =>
      val outputJarPath = root / "hello.jar"
      os.proc(
        TestUtil.cli,
        "--power",
        "package",
        extraOptions,
        "--library",
        s"$mainClass.scala",
        "--resource-dir",
        resourceDir,
        "-o",
        outputJarPath
      )
        .call(cwd = root)
      expect(os.isFile(outputJarPath))
      val res =
        os.proc(
          TestUtil.cli,
          "run",
          "--jar",
          outputJarPath,
          "--main-class",
          mainClass,
          "--dep",
          "org.scala-lang.modules::scala-xml:2.2.0",
          extraOptions
        ).call(cwd = root)
      expect(res.out.trim() == child)
    }
  }
}
