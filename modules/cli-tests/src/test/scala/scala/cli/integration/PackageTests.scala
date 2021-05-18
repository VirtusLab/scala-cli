package scala.cli.integration

import java.nio.file.Files

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
      assert(os.isFile(launcher))
      assert(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
      assert(output == message)
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
      assert(os.isFile(launcher))

      val nodePath = TestUtil.fromPath("node").getOrElse("node")
      val output = os.proc(nodePath, launcher.toString).call(cwd = root).out.text.trim
      assert(output == message)
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
      assert(os.isFile(launcher))
      assert(Files.isExecutable(launcher.toNIO))

      val output = os.proc(launcher.toString).call(cwd = root).out.text.trim
      assert(output == message)
    }
  }

  if (!Properties.isWin)
    test("simple native") {
      simpleNativeTest()
    }

}
