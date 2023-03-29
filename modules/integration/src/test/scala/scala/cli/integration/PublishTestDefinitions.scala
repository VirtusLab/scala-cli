package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Paths
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters.*
import scala.util.Properties

abstract class PublishTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  protected def extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  private object TestCase {
    val testInputs: TestInputs = TestInputs(
      os.rel / "project" / "foo" / "Hello.scala" ->
        """//> using publish.organization "org.virtuslab.scalacli.test"
          |//> using publish.name "simple"
          |//> using publish.version "0.2.0-SNAPSHOT"
          |//> using publish.url "https://github.com/VirtusLab/scala-cli"
          |//> using publish.license "Apache 2.0:http://opensource.org/licenses/Apache-2.0"
          |//> using publish.developer "someone|Someone||https://github.com/someone"
          |
          |package foo
          |
          |object Hello {
          |  def main(args: Array[String]): Unit =
          |    println(Messages.hello)
          |}
          |""".stripMargin,
      os.rel / "project" / "foo" / "Messages.scala" ->
        """package foo
          |
          |object Messages {
          |  def hello = "Hello"
          |}
          |""".stripMargin
    )
    val scalaSuffix: String =
      if (actualScalaVersion.startsWith("3.")) "_3"
      else "_" + actualScalaVersion.split('.').take(2).mkString(".")
    val expectedArtifactsDir: os.RelPath =
      os.rel / "org" / "virtuslab" / "scalacli" / "test" / s"simple$scalaSuffix" / "0.2.0-SNAPSHOT"
    val expectedJsArtifactsDir: os.RelPath =
      os.rel / "org" / "virtuslab" / "scalacli" / "test" / s"simple_sjs1$scalaSuffix" / "0.2.0-SNAPSHOT"
  }

  val baseExpectedArtifacts = Set(
    s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.pom",
    s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.jar",
    s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT-javadoc.jar",
    s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT-sources.jar"
  )

  val expectedArtifacts = baseExpectedArtifacts
    .flatMap { n =>
      Seq(n, n + ".asc")
    }
    .flatMap { n =>
      Seq("", ".md5", ".sha1").map(n + _)
    }
    .map(os.rel / _)

  val expectedSourceEntries = Set(
    "foo/Hello.scala",
    "foo/Messages.scala"
  )

  test("simple") {

    val publicKey = {
      val uri = Thread.currentThread().getContextClassLoader
        .getResource("test-keys/key.asc")
        .toURI
      os.Path(Paths.get(uri))
    }
    val secretKey = {
      val uri = Thread.currentThread().getContextClassLoader
        .getResource("test-keys/key.skr")
        .toURI
      os.Path(Paths.get(uri))
    }

    // format: off
    val signingOptions = Seq(
      "--secret-key", s"file:$secretKey",
      "--secret-key-password", "value:1234",
      "--signer", "bc"
    )
    // format: on

    TestCase.testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        signingOptions,
        "project",
        "-R",
        "test-repo"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val files = os.walk(root / "test-repo")
        .filter(os.isFile(_))
        .map(_.relativeTo(root / "test-repo"))
      val notInDir = files.filter(!_.startsWith(TestCase.expectedArtifactsDir))
      expect(notInDir.isEmpty)

      val files0 = files.map(_.relativeTo(TestCase.expectedArtifactsDir)).toSet

      expect((files0 -- expectedArtifacts).isEmpty)
      expect((expectedArtifacts -- files0).isEmpty)
      expect(files0 == expectedArtifacts) // just in case…

      val repoArgs =
        Seq[os.Shellable]("-r", "!central", "-r", (root / "test-repo").toNIO.toUri.toASCIIString)
      val dep    = s"org.virtuslab.scalacli.test:simple${TestCase.scalaSuffix}:0.2.0-SNAPSHOT"
      val res    = os.proc(TestUtil.cs, "launch", repoArgs, dep).call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello")

      val sourceJarViaCsStr =
        os.proc(TestUtil.cs, "fetch", repoArgs, "--sources", "--intransitive", dep)
          .call(cwd = root)
          .out.trim()
      val sourceJarViaCs = os.Path(sourceJarViaCsStr, os.pwd)
      val zf             = new ZipFile(sourceJarViaCs.toIO)
      val entries        = zf.entries().asScala.toVector.map(_.getName).toSet
      expect(entries == expectedSourceEntries)

      val signatures = expectedArtifacts.filter(_.last.endsWith(".asc"))
      assert(signatures.nonEmpty)
      val verifyProc = os.proc(
        TestUtil.cli,
        "--power",
        "pgp",
        "verify",
        "--key",
        publicKey,
        signatures.map(os.rel / "test-repo" / TestCase.expectedArtifactsDir / _)
      )
        .call(cwd = root, mergeErrIntoOut = true)

      expect(!verifyProc.out.text().contains(s"invalid signature"))
    }
  }

  test("artifacts name for scalajs") {
    val baseExpectedArtifacts = Seq(
      s"simple_sjs1${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.pom",
      s"simple_sjs1${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.jar",
      s"simple_sjs1${TestCase.scalaSuffix}-0.2.0-SNAPSHOT-javadoc.jar",
      s"simple_sjs1${TestCase.scalaSuffix}-0.2.0-SNAPSHOT-sources.jar"
    )
    val expectedArtifacts = baseExpectedArtifacts
      .flatMap { n =>
        Seq("", ".md5", ".sha1").map(n + _)
      }
      .map(os.rel / _)
      .toSet

    TestCase.testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        "project",
        "--js",
        "-R",
        "test-repo"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val files = os.walk(root / "test-repo")
        .filter(os.isFile(_))
        .map(_.relativeTo(root / "test-repo"))
      val notInDir = files.filter(!_.startsWith(TestCase.expectedJsArtifactsDir))
      expect(notInDir.isEmpty)

      val files0 = files.map(_.relativeTo(TestCase.expectedJsArtifactsDir)).toSet

      expect((files0 -- expectedArtifacts).isEmpty)
      expect((expectedArtifacts -- files0).isEmpty)
    }
  }

  test("custom checksums") {
    val baseExpectedArtifacts = Seq(
      s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.pom",
      s"simple${TestCase.scalaSuffix}-0.2.0-SNAPSHOT.jar"
    )
    val expectedArtifacts = baseExpectedArtifacts
      .flatMap { n =>
        Seq("", ".sha1").map(n + _)
      }
      .map(os.rel / _)
      .toSet

    TestCase.testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        "--sources=false",
        "--doc=false",
        "--checksum",
        "sha-1",
        "project",
        "-R",
        "test-repo"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit
      )

      val files = os.walk(root / "test-repo")
        .filter(os.isFile(_))
        .map(_.relativeTo(root / "test-repo"))
      val notInDir = files.filter(!_.startsWith(TestCase.expectedArtifactsDir))
      expect(notInDir.isEmpty)

      val files0 = files.map(_.relativeTo(TestCase.expectedArtifactsDir)).toSet

      expect((files0 -- expectedArtifacts).isEmpty)
      expect((expectedArtifacts -- files0).isEmpty)
      expect(files0 == expectedArtifacts) // just in case…
    }
  }

  test("correctly list main classes") {
    val (scalaFile1, scalaFile2, scriptName) = ("ScalaMainClass1", "ScalaMainClass2", "ScalaScript")
    val scriptsDir                           = "scripts"
    val inputs = TestInputs(
      os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
      os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
      os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        ".",
        "--list-main-classes"
      )
        .call(cwd = root)
      val output = res.out.trim()
      val resLocal = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "local",
        extraOptions,
        ".",
        "--list-main-classes"
      )
        .call(cwd = root)
      val outputLocal = resLocal.out.trim()
      expect(output == outputLocal)
      val mainClasses = output.linesIterator.toSeq.last.split(" ").toSet
      expect(mainClasses == Set(scalaFile1, scalaFile2, s"$scriptsDir.${scriptName}_sc"))
    }
  }

  test("no secret key password") {
    // format: off
    val signingOptions = Seq(
      "--secret-key", s"file:key.skr",
      "--signer", "bc"
    )
    // format: on

    TestCase.testInputs.fromRoot { root =>
      val confDir  = root / "config"
      val confFile = confDir / "test-config.json"

      os.write(confFile, "{}", createFolders = true)

      if (!Properties.isWin)
        os.perms.set(confDir, "rwx------")

      val extraEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      os.proc(
        TestUtil.cli,
        "--power",
        "pgp",
        "create",
        "--email",
        "some_email"
      ).call(cwd = root, env = extraEnv)

      val publicKey = os.Path("key.pub", root)

      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        signingOptions,
        "project",
        "-R",
        "test-repo"
      ).call(cwd = root, env = extraEnv)

      val files = os.walk(root / "test-repo")
        .filter(os.isFile(_))
        .map(_.relativeTo(root / "test-repo"))
      val notInDir = files.filter(!_.startsWith(TestCase.expectedArtifactsDir))
      expect(notInDir.isEmpty)

      val files0 = files.map(_.relativeTo(TestCase.expectedArtifactsDir)).toSet

      expect((files0 -- expectedArtifacts).isEmpty)
      expect((expectedArtifacts -- files0).isEmpty)
      expect(files0 == expectedArtifacts) // just in case…

      val repoArgs =
        Seq[os.Shellable]("-r", "!central", "-r", (root / "test-repo").toNIO.toUri.toASCIIString)
      val dep    = s"org.virtuslab.scalacli.test:simple${TestCase.scalaSuffix}:0.2.0-SNAPSHOT"
      val res    = os.proc(TestUtil.cs, "launch", repoArgs, dep).call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello")

      val sourceJarViaCsStr =
        os.proc(TestUtil.cs, "fetch", repoArgs, "--sources", "--intransitive", dep)
          .call(cwd = root)
          .out.trim()
      val sourceJarViaCs = os.Path(sourceJarViaCsStr, os.pwd)
      val zf             = new ZipFile(sourceJarViaCs.toIO)
      val entries        = zf.entries().asScala.toVector.map(_.getName).toSet
      expect(entries == expectedSourceEntries)

      val signatures = expectedArtifacts.filter(_.last.endsWith(".asc"))
      assert(signatures.nonEmpty)
      val verifyProc = os.proc(
        TestUtil.cli,
        "--power",
        "pgp",
        "verify",
        "--key",
        publicKey,
        signatures.map(os.rel / "test-repo" / TestCase.expectedArtifactsDir / _)
      ).call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

      expect(!verifyProc.out.text().contains(s"invalid signature"))
    }
  }

  if (!Properties.isWin) // TODO: fix intermittent failures on Windows
    test("secret keys in config") {

      TestCase.testInputs.fromRoot { root =>
        val confDir  = root / "config"
        val confFile = confDir / "test-config.json"

        os.write(confFile, "{}", createFolders = true)

        if (!Properties.isWin)
          os.perms.set(confDir, "rwx------")

        val extraEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

        os.proc(
          TestUtil.cli,
          "--power",
          "config",
          "--create-pgp-key",
          "--email",
          "some_email"
        ).call(cwd = root, env = extraEnv)

        TestCase.testInputs.fromRoot { root =>
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            extraOptions,
            "--signer",
            "bc",
            "project",
            "-R",
            "test-repo"
          ).call(
            cwd = root,
            stdin = os.Inherit,
            stdout = os.Inherit,
            env = extraEnv
          )

          val files = os.walk(root / "test-repo")
            .filter(os.isFile(_))
            .map(_.relativeTo(root / "test-repo"))
          val notInDir = files.filter(!_.startsWith(TestCase.expectedArtifactsDir))
          expect(notInDir.isEmpty)

          val files0 = files.map(_.relativeTo(TestCase.expectedArtifactsDir)).toSet

          expect((files0 -- expectedArtifacts).isEmpty)
          expect((expectedArtifacts -- files0).isEmpty)
          expect(files0 == expectedArtifacts) // just in case…

          val repoArgs =
            Seq[os.Shellable](
              "-r",
              "!central",
              "-r",
              (root / "test-repo").toNIO.toUri.toASCIIString
            )
          val dep    = s"org.virtuslab.scalacli.test:simple${TestCase.scalaSuffix}:0.2.0-SNAPSHOT"
          val res    = os.proc(TestUtil.cs, "launch", repoArgs, dep).call(cwd = root)
          val output = res.out.trim()
          expect(output == "Hello")

          val sourceJarViaCsStr =
            os.proc(TestUtil.cs, "fetch", repoArgs, "--sources", "--intransitive", dep)
              .call(cwd = root)
              .out.trim()
          val sourceJarViaCs = os.Path(sourceJarViaCsStr, os.pwd)
          val zf             = new ZipFile(sourceJarViaCs.toIO)
          val entries        = zf.entries().asScala.toVector.map(_.getName).toSet
          expect(entries == expectedSourceEntries)

          val publicKey = os.proc(
            TestUtil.cli,
            "--power",
            "config",
            "pgp.public-key"
          ).call(cwd = root, env = extraEnv)
            .out.trim()
            .stripPrefix("value:")

          os.write(os.Path("key.pub", root), publicKey)

          val signatures = expectedArtifacts.filter(_.last.endsWith(".asc"))
          assert(signatures.nonEmpty)
          val verifyProc = os.proc(
            TestUtil.cli,
            "--power",
            "pgp",
            "verify",
            "--key",
            s"key.pub",
            signatures.map(os.rel / "test-repo" / TestCase.expectedArtifactsDir / _)
          )
            .call(cwd = root, env = extraEnv, mergeErrIntoOut = true)

          expect(!verifyProc.out.text().contains(s"invalid signature"))
        }
      }
    }

  test("signer=none overrides other options") {

    TestCase.testInputs.fromRoot { root =>
      val confDir  = root / "config"
      val confFile = confDir / "test-config.json"

      os.write(confFile, "{}", createFolders = true)

      if (!Properties.isWin)
        os.perms.set(confDir, "rwx------")

      val extraEnv = Map("SCALA_CLI_CONFIG" -> confFile.toString)

      os.proc(
        TestUtil.cli,
        "--power",
        "config",
        "--create-pgp-key",
        "--email",
        "some_email"
      ).call(cwd = root, env = extraEnv)

      TestCase.testInputs.fromRoot { root =>
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          extraOptions,
          "--secret-key",
          "value:INCORRECT_KEY",
          "--signer",
          "none",
          "project",
          "-R",
          "test-repo"
        ).call(
          cwd = root,
          stdin = os.Inherit,
          stdout = os.Inherit,
          env = extraEnv
        )

        val files = os.walk(root / "test-repo")
          .filter(os.isFile(_))
          .map(_.relativeTo(root / "test-repo"))
        val notInDir = files.filter(!_.startsWith(TestCase.expectedArtifactsDir))
        expect(notInDir.isEmpty)

        val files0 = files.map(_.relativeTo(TestCase.expectedArtifactsDir)).toSet

        val expectedArtifactsNotSigned = expectedArtifacts.filterNot(_.last.contains(".asc"))

        expect((files0 -- expectedArtifactsNotSigned).isEmpty)
        expect((expectedArtifactsNotSigned -- files0).isEmpty)
        expect(files0 == expectedArtifactsNotSigned) // just in case…
      }
    }
  }
}
