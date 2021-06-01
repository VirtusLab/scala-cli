package scala.cli.integration

import java.nio.file.Files

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class PackageTests extends munit.FunSuite {

  test("simple script") {
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
    val launcherName = {
      val ext = if (Properties.isWin) ".bat" else ""
      fileName.stripSuffix(".sc") + ext
    }
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", fileName).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        stderr = os.Inherit
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
      os.proc(TestUtil.cli, "package").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        stderr = os.Inherit
      )

      val outputName = if (Properties.isWin) "app.bat" else "app"
      val launcher = root / outputName

      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
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
    val destName = fileName.stripSuffix(".sc") + ".js"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "package", fileName, "--js").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        stderr = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))

      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (!TestUtil.isNativeCli || !Properties.isWin)
    test("simple JS") {
      simpleJsTest()
    }

  def simpleNativeTest(): Unit = {
    val fileName = "simple.sc"
    val message = "Hello"
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
      os.proc(TestUtil.cli, "package", fileName, "--native").call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        stderr = os.Inherit
      )

      val launcher = root / destName
      expect(os.isFile(launcher))
      expect(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
      expect(output == message)
    }
  }

  if (!Properties.isWin)
    test("simple native") {
      simpleNativeTest()
    }

}
