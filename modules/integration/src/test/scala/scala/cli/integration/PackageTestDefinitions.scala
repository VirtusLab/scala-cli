package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.{File, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Arrays
import java.util.regex.Pattern
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters._
import scala.util.Properties

abstract class PackageTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  def maybeUseBash(cmd: os.Shellable*)(cwd: os.Path = null): os.CommandResult = {
    val res = os.proc(cmd: _*).call(cwd = cwd, check = false)
    if (Properties.isLinux && res.exitCode == 127)
      // /bin/sh seems to have issues with '%' signs in PATH, that coursier can leave
      // in the JVM path entry (https://unix.stackexchange.com/questions/126955/percent-in-path-environment-variable)
      os.proc(("/bin/bash": os.Shellable) +: cmd: _*).call(cwd = cwd)
    else {
      expect(res.exitCode == 0)
      res
    }
  }

  test("simple script") {
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
    val launcherName = {
      val ext = if (Properties.isWin) ".bat" else ""
      fileName.stripSuffix(".sc") + ext
    }
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, fileName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / launcherName

      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = maybeUseBash(launcher)(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  test("current directory as default input") {
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
      os.proc(TestUtil.cli, "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "simple.bat" else "simple"
      val launcher   = root / outputName

      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = maybeUseBash(launcher.toString)(cwd = root).out.text().trim
      expect(output == message)
    }
  }
  test("resource directory for coursier bootstrap launcher") {
    val fileName = "hello.sc"
    val message  = "1,2,3"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""|//> using resourceDir "."
              |import scala.io.Source
              |
              |val inputs = Source.fromResource("input").getLines.toSeq
              |println(inputs.mkString)
              |""".stripMargin,
        os.rel / "input" -> message
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "hello.bat" else "hello"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  test("resource directory for library package") {
    val fileName     = "MyLibrary.scala"
    val outputLib    = "my-library.jar"
    val resourceFile = "input"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""|//> using resourceDir "."
              |
              |class MyLibrary {
              |  def message = "Hello"
              |}
              |""".stripMargin,
        os.rel / resourceFile -> "1,2,3"
      )
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, ".", "-o", outputLib, "--library").call(
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

      os.proc(TestUtil.cli, "package", zipPath, extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "hello.bat" else "hello"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
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
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, fileName, "--js").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))

      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      val output   = os.proc(nodePath, launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  def sourceMapJsTest(): Unit = {
    val fileName = "simple.sc"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""import scala.scalajs.js
             |println("Hello World")
             |""".stripMargin
      )
    )
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
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
      expect(os.isFile(expectedSourceMapsPath))
    }
  }

  def multiModulesJsTest(): Unit = {
    val fileName = "Hello.scala"
    val message  = "Hello World from JS"
    val inputs = TestInputs(
      Seq(
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
    )
    val destDir = fileName.stripSuffix(".scala")
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, fileName, "--js", "-o", destDir).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destDir / "main.js"
      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      os.write(root / "package.json", "{\n\n  \"type\": \"module\"\n\n}") // enable es module
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
  }
  def smallModulesJsTest(): Unit = {
    val fileName = "Hello.scala"
    val message  = "Hello World from JS"
    val inputs = TestInputs(
      Seq(
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
    )
    val destDir = fileName.stripSuffix(".scala")
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
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
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  def jsHeaderTest(): Unit = {
    val fileName        = "Hello.scala"
    val jsHeader        = "#!/usr/bin/env node"
    val jsHeaderNewLine = s"$jsHeader\\n"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""|//> using jsHeader "$jsHeaderNewLine"
              |//> using jsMode "release"
              |
              |object Hello extends App {
              |  println("Hello")
              |}
              |""".stripMargin
      )
    )
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, fileName, "--js", "-o", destName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher        = root / destName
      val launcherContent = os.read(launcher)
      expect(launcherContent.startsWith(jsHeader))
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
    test("small modules js") {
      smallModulesJsTest()
    }
    test("js header in release mode") {
      jsHeaderTest()
    }
  }

  def simpleNativeTest(): Unit = {
    val fileName   = "simple.sc"
    val message    = "Hello"
    val platformNl = if (Properties.isWin) "\\r\\n" else "\\n"
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
    val destName = {
      val ext = if (Properties.isWin) ".exe" else ""
      fileName.stripSuffix(".sc") + ext
    }
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, fileName, "--native").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.text().trim
      expect(output == message)
    }
  }

  if (!Properties.isWin && actualScalaVersion.startsWith("2.13"))
    test("simple native") {
      simpleNativeTest()
    }

  test("assembly") {
    val fileName = "simple.sc"
    val message  = "Hello"
    val inputs = TestInputs(
      Seq(
        os.rel / fileName ->
          s"""import $$ivy.`org.typelevel::cats-kernel:2.6.1`
             |import cats.kernel._
             |val m = Monoid.instance[String]("", (a, b) => a + b)
             |val msgStuff = m.combineAll(List("$message", "", ""))
             |println(msgStuff)
             |""".stripMargin
      )
    )
    val launcherName = fileName.stripSuffix(".sc") + ".jar"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, "--assembly", fileName).call(
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

      val output = maybeUseBash(runnableLauncher.toString)(cwd = root).out.text().trim
      val maxRunnableLauncherSize = 1024 * 1024 * 12 // should be smaller than 12MB
      expect(output == message)
      expect(runnableLauncherSize < maxRunnableLauncherSize)
    }
  }

  test("assembly no preamble") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""package hello
             |
             |object Hello {
             |  def main(args: Array[String]): Unit =
             |    println("Hello from " + "assembly")
             |}
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
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
      expect(!Arrays.equals(contentStart, preambleStart))

      val output = os.proc("java", "-cp", launcher, "hello.Hello")
        .call(cwd = root).out.text().trim
      expect(output == "Hello from assembly")
    }
  }

  test("assembly provided") {
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""package hello
             |
             |object Hello {
             |  def main(args: Array[String]): Unit =
             |    println("Hello from Scala " + scala.util.Properties.versionNumberString)
             |}
             |""".stripMargin
      )
    )
    val providedModule =
      if (actualScalaVersion.startsWith("2.")) "org.scala-lang:scala-library"
      else "org.scala-lang:scala3-library_3"
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
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
          .call(cwd = root).out.text().trim
      val output =
        os.proc("java", "-cp", s"$launcher${File.pathSeparator}$scalaLibCp", "hello.Hello")
          .call(cwd = root).out.text().trim
      val expectedScalaVerInOutput =
        if (actualScalaVersion.startsWith("2.")) actualScalaVersion
        else {
          val scalaLibJarName = scalaLibCp.split(File.pathSeparator)
            .map(_.split(Pattern.quote(File.separator)).last)
            .filter(_.startsWith("scala-library-"))
            .headOption
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
      Seq(
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
    )
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, ".").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val outputName = if (Properties.isWin) "Main.bat" else "Main"
      val launcher   = root / outputName

      val output = os.proc(launcher.toString).call(cwd = root).out.text().trim
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
    Seq(
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
  )
  test("source JAR") {
    val dest = os.rel / "sources.jar"
    simpleInputWithScalaAndSc.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, ".", "-o", dest, "--source").call(
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
      expect(genContentStr.contains("object simple {"))
    }
  }

  test("doc JAR") {
    val dest = os.rel / "doc.jar"
    simpleInputWithScalaAndSc.fromRoot { root =>
      os.proc(TestUtil.cli, "package", extraOptions, ".", "-o", dest, "--doc").call(
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
            "_empty_/simple$.html",
            "lib/Messages$.html"
          )
      val entries = zf.entries().asScala.iterator.map(_.getName).toSet
      expect(expectedEntries.forall(e => entries.contains(e)))
    }
  }

  test("native image") {
    val message = "Hello from native-image"
    val dest =
      if (Properties.isWin) "hello.exe"
      else "hello"
    val inputs = TestInputs(
      Seq(
        os.rel / "Hello.scala" ->
          s"""object Hello {
             |  def main(args: Array[String]): Unit =
             |    println("$message")
             |}
             |""".stripMargin
      )
    )
    inputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "package",
        extraOptions,
        ".",
        "--native-image",
        "-o",
        dest,
        "--",
        "--no-fallback"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      expect(os.isFile(root / dest))

      // FIXME Check that dest is indeed a binary?

      val res    = os.proc(root / dest).call(cwd = root)
      val output = res.out.text().trim
      expect(output == message)
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
        "package",
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
}
