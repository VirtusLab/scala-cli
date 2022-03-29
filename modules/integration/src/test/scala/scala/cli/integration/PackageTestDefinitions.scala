package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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

      val outputName = if (Properties.isWin) "app.bat" else "app"
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

      val outputName = if (Properties.isWin) "app.bat" else "app"
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

      val outputName = if (Properties.isWin) "app.bat" else "app"
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

      val output = maybeUseBash(runnableLauncher.toString)(cwd = root).out.text().trim
      expect(output == message)
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

      val outputName = if (Properties.isWin) "app.bat" else "app"
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
    finally if (is != null)
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
}
