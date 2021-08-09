package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class NativePackagerTests extends munit.FunSuite{

  val helloWorldFileName = "HelloWorldScalaCli.scala"
  val message = "Hello, world!"
  val licencePath = "DummyLICENSE"
  val testInputs = TestInputs(
    Seq(
      os.rel / helloWorldFileName ->
        s"""
          |object HelloWorld {
          |  def main(args: Array[String]): Unit = {
          |    println("$message")
          |  }
          |}""".stripMargin,
      os.rel / licencePath ->  "LICENSE"
    )
  )

  if (Properties.isMac) {
    test("building pkg package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase
        val pkgAppFile = s"$appName.pkg"
        os.proc(
            TestUtil.cli, 
            "package", 
            TestUtil.extraOptions, 
            helloWorldFileName, 
            "--pkg", 
            "--output", pkgAppFile, 
            "--identifier", "scala-cli", 
            "--launcher-app-name", appName
          )
          .call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

        val pkgAppPath = root / pkgAppFile
        expect(os.isFile(pkgAppPath))

        if (TestUtil.isCI) {
          os.proc("installer", "-pkg", pkgAppFile, "-target", "CurrentUserHomeDirectory").call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

          val home = sys.props("user.home")
          val output = os.proc(s"$home/Applications/$appName.app/Contents/MacOS/$appName").call(cwd = os.root).out.text.trim
          expect(output == message)
        }
      }
    }


    test("building dmg package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala")

        os.proc(
            TestUtil.cli, 
            "package", 
            TestUtil.extraOptions, 
            helloWorldFileName, 
            "--dmg", 
            "--output", appName, 
            "--identifier", "scala-cli", 
            "--launcher-app-name", appName
          )
          .call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

        val launcher = root / s"$appName.dmg"
        expect(os.isFile(launcher))

        if (TestUtil.isCI) {
          os.proc("hdiutil", "attach", launcher).call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

          val output = os.proc(s"/Volumes/$appName/$appName.app/Contents/MacOS/$appName").call(cwd = os.root).out.text.trim
          expect(output == message)

          os.proc("hdiutil", "detach", s"/Volumes/$appName").call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )
        }
      }
    }
  }

  if ( Properties.isLinux) {

    test("building deb package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala")

        os.proc(
            TestUtil.cli, 
            "package", 
            TestUtil.extraOptions, 
            helloWorldFileName, 
            "--deb", 
            "--output", s"$appName.deb",
            "--maintainer", "scala-cli-test", 
            "--description", "scala-cli-test", 
            "--launcher-app-name", appName
          )
          .call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

        val launcher = root / s"$appName.deb"
        expect(os.isFile(launcher))

        if (TestUtil.isCI) {
          os.proc("dpkg", "-x", launcher, root).call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

          val output = os.proc(s"$root/usr/share/scala/$appName").call(cwd = os.root).out.text.trim
          expect(output == message)
        }
      }
    }

    test("building rpm package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala")

        os.proc(
            TestUtil.cli, 
            "package",
            TestUtil.extraOptions, 
            helloWorldFileName,
            "--rpm", 
            "--output", s"$appName.rpm", 
            "--description", "scala-cli",
            "--license", "ASL 2.0", 
            "--version", "1.0.0", 
            "--launcher-app-name", appName
          )
          .call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

        val launcher = root / s"$appName.rpm"
        expect(os.isFile(launcher))
      }
    }
  }

  if (Properties.isWin && !TestUtil.isNativeCli)
    test("building msi package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala")

        os.proc(
            TestUtil.cli,
            "package", helloWorldFileName,
            "--msi",
            "--output", s"$appName.msi",
            "--product-name", "scala-cli",
            "--license-path", licencePath,
            "--maintainer", "Scala-cli",
            "--launcher-app-name",
            appName
          )
          .call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit
          )

        val launcher = root / s"$appName.msi"
        expect(os.isFile(launcher))
      }
    }
}
