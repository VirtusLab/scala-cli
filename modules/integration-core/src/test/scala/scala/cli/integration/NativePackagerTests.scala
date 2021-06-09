package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class NativePackagerTests extends munit.FunSuite{

  val helloWorldFileName = "HelloWorld.scala"
  val message = "Hello, world!"
  val helloWorldTestInputs = TestInputs(
    Seq(
      os.rel / helloWorldFileName ->
        s"""
          |object HelloWorld {
          |  def main(args: Array[String]): Unit = {
          |    println("$message")
          |  }
          |}""".stripMargin
    )
  )

  if (Properties.isMac) {
    test("building pkg package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--pkg", "-n", launcherName).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.pkg"
        expect(os.isFile(launcher))
      }
    }

    test("building dmg package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--dmg", "-n", launcherName).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.dmg"
        expect(os.isFile(launcher))
      }
    }
  }
}
