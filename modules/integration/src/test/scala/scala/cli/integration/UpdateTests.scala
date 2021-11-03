package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class UpdateTests extends munit.FunSuite {

  val firstVersion           = "0.0.1"
  val dummyScalaCliFirstName = "DummyScalaCli-1.scala"
  val dummyScalaCliBinName   = "scala-cli-dummy-test"
  val testInputs = TestInputs(
    Seq(
      os.rel / dummyScalaCliFirstName ->
        s"""
           |object DummyScalaCli extends App {
           |  println(\"$firstVersion\")
           |}""".stripMargin
    )
  )

  private def packageDummyScalaCli(root: os.Path, dummyScalaCliFileName: String, output: String) = {
    // format: off
    val cmd = Seq[os.Shellable](
      TestUtil.cli, "package", dummyScalaCliFileName, "-o", output
    )
    // format: on
    os.proc(cmd).call(
      cwd = root,
      stdin = os.Inherit,
      stdout = os.Inherit
    )
  }

  private def installScalaCli(
    root: os.Path,
    binVersion: String,
    binDirPath: os.Path
  ) = {
    // format: off
    val cmdInstallVersion = Seq[os.Shellable](
      TestUtil.cli, "install-home",
      "--env",
      "--scala-cli-binary-path", binVersion,
      "--binary-name", dummyScalaCliBinName,
      "--bin-dir", binDirPath,
      "--force"
    )
    // format: on
    os.proc(cmdInstallVersion).call(
      cwd = root,
      stdin = os.Inherit,
      stdout = os.Inherit
    )
  }

  def runUpdate(): Unit = {

    testInputs.fromRoot { root =>
      val binDirPath = root / ".scala" / "scala-cli"

      val binDummyScalaCliFirst = dummyScalaCliFirstName.stripSuffix(".scala").toLowerCase

      packageDummyScalaCli(root, dummyScalaCliFirstName, binDummyScalaCliFirst)

      // install 1 version
      installScalaCli(root, binDummyScalaCliFirst, binDirPath)

      val v1Install = os.proc(binDirPath / dummyScalaCliBinName).call(
        cwd = root,
        stdin = os.Inherit
      ).out.text().trim
      expect(v1Install == firstVersion)

      // update to newest version
      // format: off
      val cmdUpdate = Seq[os.Shellable](
        TestUtil.cli,
        "update",
        "--binary-name", dummyScalaCliBinName,
        "--bin-dir", binDirPath,
        "--force"
      )
      // format: on
      os.proc(cmdUpdate).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val nextVersion = os.proc(binDirPath / dummyScalaCliBinName, "version").call(
        cwd = root,
        stdin = os.Inherit
      ).out.text().trim

      expect(firstVersion != nextVersion)
    }
  }

  if (!Properties.isWin)
    test("updating dummy scala-cli using update command") {
      runUpdate()
    }

  test("run update before run/test/compile should not return exit code") {
    val res = os.proc(TestUtil.cli, "update", "--is-internal-run").call(cwd = os.pwd)
    expect(res.exitCode == 0)
  }

}
