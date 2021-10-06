package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class InstallHomeTests extends munit.FunSuite {

  val firstVersion            = "0.0.1"
  val secondVersion           = "0.0.2"
  val dummyScalaCliFirstName  = "DummyScalaCli-1.scala"
  val dummyScalaCliSecondName = "DummyScalaCli-2.scala"
  val dummyScalaCliBinName    = "scala-cli-dummy-test"
  val testInputs = TestInputs(
    Seq(
      os.rel / dummyScalaCliFirstName ->
        s"""
           |object DummyScalaCli extends App {
           |  println(\"$firstVersion\")
           |}""".stripMargin,
      os.rel / dummyScalaCliSecondName ->
        s"""
           |object DummyScalaCli extends App {
           |  println(\"$secondVersion\")
           |}""".stripMargin
    )
  )
  val binDirPath = os.home / ".scala" / "scala-cli"

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

  private def installScalaCli(root: os.Path, binVersion: String, force: Boolean) = {
    // format: off
    val cmdInstallVersion = Seq[os.Shellable](
      TestUtil.cli, "install-home", "--scala-cli-binary-path", binVersion , "--binary-name", dummyScalaCliBinName, "--bin-dir", binDirPath
    ) ++ (if(force) Seq[os.Shellable]("--force") else Seq.empty)
    // format: on
    os.proc(cmdInstallVersion).call(
      cwd = root,
      stdin = os.Inherit,
      stdout = os.Inherit
    )
  }

  if (!Properties.isWin && TestUtil.isCI) {
    test("updating and downgrading dummy scala-cli using install-home command") {

      testInputs.fromRoot { root =>

        val binDummyScalaCliFirst  = dummyScalaCliFirstName.stripSuffix(".scala").toLowerCase
        val binDummyScalaCliSecond = dummyScalaCliSecondName.stripSuffix(".scala").toLowerCase

        packageDummyScalaCli(root, dummyScalaCliFirstName, binDummyScalaCliFirst)
        packageDummyScalaCli(root, dummyScalaCliSecondName, binDummyScalaCliSecond)

        // install 1 version
        installScalaCli(root, binDummyScalaCliFirst, force = true)

        println(binDirPath / dummyScalaCliBinName)

        val v1Install = os.proc(binDirPath / dummyScalaCliBinName).call(
          cwd = root,
          stdin = os.Inherit
        ).out.text.trim
        expect(v1Install == firstVersion)

        // update to 2 version
        installScalaCli(root, binDummyScalaCliSecond, force = false)

        val v2Update = os.proc(binDirPath / dummyScalaCliBinName).call(
          cwd = root,
          stdin = os.Inherit
        ).out.text.trim
        expect(v2Update == secondVersion)

        // downgrade to 1 version with force
        installScalaCli(root, binDummyScalaCliFirst, force = true)

        val v1Downgrade = os.proc(binDirPath / dummyScalaCliBinName).call(
          cwd = root,
          stdin = os.Inherit
        ).out.text.trim
        expect(v1Downgrade == firstVersion)
      }
    }
  }
}
