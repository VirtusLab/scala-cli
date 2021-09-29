package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Files
import java.util.zip.ZipFile

import scala.util.Properties

abstract class PackageTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

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

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
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

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
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
      val output   = os.proc(nodePath, launcher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (!TestUtil.isNativeCli || !Properties.isWin)
    test("simple JS") {
      simpleJsTest()
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

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (!Properties.isWin && actualScalaVersion.startsWith("2."))
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
      finally {
        if (zf != null)
          zf.close()
      }

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

      val output = os.proc(runnableLauncher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

}
