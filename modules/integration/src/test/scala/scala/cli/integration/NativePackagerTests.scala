package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class NativePackagerTests extends munit.FunSuite {

  val helloWorldFileName = "HelloWorldScalaCli.scala"
  val message            = "Hello, world!"
  val licencePath        = "DummyLICENSE"
  val testInputs = TestInputs(
    Seq(
      os.rel / helloWorldFileName ->
        s"""
           |object HelloWorld {
           |  def main(args: Array[String]): Unit = {
           |    println("$message")
           |  }
           |}""".stripMargin,
      os.rel / licencePath -> "LICENSE"
    )
  )

  if (Properties.isMac) {
    test("building pkg package") {

      testInputs.fromRoot { root =>

        val appName    = helloWorldFileName.stripSuffix(".scala").toLowerCase
        val pkgAppFile = s"$appName.pkg"
        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "package", TestUtil.extraOptions, helloWorldFileName, "--pkg",
          "--output", pkgAppFile,
          "--identifier", "scala-cli",
          "--launcher-app", appName
        )
        // format: on
        os.proc(cmd).call(
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
          val output = os.proc(s"$home/Applications/$appName.app/Contents/MacOS/$appName")
            .call(cwd = os.root)
            .out.text.trim
          expect(output == message)
        }
      }
    }

    test("building dmg package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase()

        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "package", TestUtil.extraOptions, helloWorldFileName, "--dmg",
          "--output", appName,
          "--identifier", "scala-cli",
          "--launcher-app", appName
        )
        // format: on
        os.proc(cmd).call(
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

          val output = os.proc(s"/Volumes/$appName/$appName.app/Contents/MacOS/$appName")
            .call(cwd = os.root)
            .out.text.trim
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

  if (Properties.isLinux) {

    test("building deb package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase()

        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "package", TestUtil.extraOptions, helloWorldFileName, "--deb",
          "--output", s"$appName.deb",
          "--maintainer", "scala-cli-test",
          "--description", "scala-cli-test",
          "--launcher-app", appName
        )
        // format: on
        os.proc(cmd).call(
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

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase()

        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "package", TestUtil.extraOptions, helloWorldFileName, "--rpm",
          "--output", s"$appName.rpm",
          "--description", "scala-cli",
          "--license", "ASL 2.0",
          "--version", "1.0.0",
          "--launcher-app", appName
        )
        // format: on
        os.proc(cmd).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )

        val launcher = root / s"$appName.rpm"
        expect(os.isFile(launcher))
      }
    }
  }

  if (Properties.isWin)
    test("building msi package") {

      testInputs.fromRoot { root =>

        val appName = helloWorldFileName.stripSuffix(".scala").toLowerCase()

        // format: off
        val cmd = Seq[os.Shellable](
          TestUtil.cli, "package", helloWorldFileName, "--msi",
          "--output", s"$appName.msi",
          "--product-name", "scala-cli",
          "--license-path", licencePath,
          "--maintainer", "Scala-cli",
          "--launcher-app", appName,
          "--suppress-validation"
        )
        // format: on
        os.proc(cmd).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )

        val launcher = root / s"$appName.msi"
        expect(os.isFile(launcher))
      }
    }

  def runTest(): Unit =
    testInputs.fromRoot { root =>

      val appName         = helloWorldFileName.stripSuffix(".scala")
      val imageRepository = appName.toLowerCase()
      val imageTag        = "latest"

      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "package", helloWorldFileName,
        "--docker",
        "--docker-image-repository", imageRepository,
        "--docker-image-tag", imageTag
      )
      // format: on

      os.proc(cmd)
        .call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )

      val expectedImage =
        s"$imageRepository:$imageTag"

      try {
        val output = os.proc("docker", "run", expectedImage).call(cwd = os.root).out.text.trim
        expect(output == message)
      }
      // clear
      finally os.proc("docker", "rmi", "-f", expectedImage).call(cwd = os.root)
    }

  def runJsTest(): Unit =
    testInputs.fromRoot { root =>

      val appName         = helloWorldFileName.stripSuffix(".scala")
      val imageRepository = appName.toLowerCase()
      val imageTag        = "latest"

      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "package", helloWorldFileName,
        "--js",
        "--docker",
        "--docker-image-repository", imageRepository,
        "--docker-image-tag", imageTag
      )
      // format: on

      os.proc(cmd)
        .call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )

      val expectedImage =
        s"$imageRepository:$imageTag"

      try {
        val output = os.proc("docker", "run", expectedImage).call(cwd = os.root).out.text.trim
        expect(output == message)

      }
      // clear
      finally os.proc("docker", "rmi", "-f", expectedImage).call(cwd = os.root)
    }

  def runNativeTest(): Unit =
    testInputs.fromRoot { root =>

      val appName         = helloWorldFileName.stripSuffix(".scala")
      val imageRepository = appName.toLowerCase()
      val imageTag        = "latest"

      // format: off
      val cmd = Seq[os.Shellable](
        TestUtil.cli,
        "package", helloWorldFileName,
        "--native",
        "-S", "2.13",
        "--docker",
        "--docker-image-repository", imageRepository,
        "--docker-image-tag", imageTag
      )
      // format: on

      os.proc(cmd)
        .call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit
        )

      val expectedImage =
        s"$imageRepository:$imageTag"

      try {
        val output = os.proc("docker", "run", expectedImage).call(cwd = os.root).out.text.trim
        expect(output == message)

      }
      // clear
      finally os.proc("docker", "rmi", "-f", expectedImage).call(cwd = os.root)
    }

  val hasDocker =
    Properties.isLinux ||
    // no docker command or no Linux from it on Github actions macOS / Windows runners
    ((Properties.isMac || Properties.isWin) && !TestUtil.isCI)

  if (hasDocker) {
    test("building docker image") {
      TestUtil.retryOnCi() {
        runTest()
      }
    }

    test("building docker image with scala.js app") {
      TestUtil.retryOnCi() {
        runJsTest()
      }
    }
  }

  if (Properties.isLinux)
    test("building docker image with scala native app") {
      TestUtil.retryOnCi() {
        runNativeTest()
      }
    }

}
