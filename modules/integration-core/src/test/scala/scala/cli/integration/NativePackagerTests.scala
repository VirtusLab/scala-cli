package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class NativePackagerTests extends munit.FunSuite{

  val helloWorldFileName = "HelloWorldScalaCli.scala"
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

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase
        val pkgAppFile = s"$appName.pkg"
        os.proc(TestUtil.cli, "package", helloWorldFileName, "--pkg", "--output", pkgAppFile).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val pkgAppPath = root / pkgAppFile
        expect(os.isFile(pkgAppPath))

        if (TestUtil.isCI) {
          os.proc("installer", "-pkg", pkgAppFile, "-target", "CurrentUserHomeDirectory").call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )

          val home = sys.props("user.home")
          val output = os.proc(s"$home/Applications/$appName.app/Contents/MacOS/$appName").call(cwd = os.root).out.text.trim
          expect(output == message)
        }
      }
    }


    test("building dmg package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--dmg", "--output", launcherName).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.dmg"
        expect(os.isFile(launcher))

        if (TestUtil.isCI) {
          os.proc("hdiutil", "attach", launcher).call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )

          val output = os.proc(s"/Volumes/$launcherName/$launcherName.app/Contents/MacOS/$launcherName").call(cwd = os.root).out.text.trim
          expect(output == message)

          os.proc("hdiutil", "detach", s"/Volumes/$launcherName").call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )
        }
      }
    }
  }

  if ( Properties.isLinux) {

    test("building deb package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--deb", "--output", s"$launcherName.deb").call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.deb"
        expect(os.isFile(launcher))

        if (TestUtil.isCI) {
          os.proc("dpkg", "-x", launcher, root).call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )

          val output = os.proc(s"$root/usr/share/scala/$launcherName").call(cwd = os.root).out.text.trim
          expect(output == message)
        }
      }
    }

    test("building rpm package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--rpm", "--output", s"$launcherName.rpm").call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.rpm"
        expect(os.isFile(launcher))
      }
    }
  }

  if ( Properties.isWin) {

    test("building msi package") {

      helloWorldTestInputs.fromRoot { root =>

        val launcherName = helloWorldFileName.stripSuffix(".scala")

        os.proc(TestUtil.cli, "package", helloWorldFileName, "--msi", "--output", s"$launcherName.msi").call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          stderr = os.Inherit
        )

        val launcher = root / s"$launcherName.msi"
        expect(os.isFile(launcher))
      }
    }
  }
}
