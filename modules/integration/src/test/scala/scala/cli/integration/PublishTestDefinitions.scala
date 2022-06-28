package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Paths
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters._

abstract class PublishTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  protected def extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  private object TestCase {
    val testInputs = TestInputs(
      Seq(
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
    )
    val scalaSuffix =
      if (actualScalaVersion.startsWith("3.")) "_3"
      else "_" + actualScalaVersion.split('.').take(2).mkString(".")
    val expectedArtifactsDir =
      os.rel / "org" / "virtuslab" / "scalacli" / "test" / s"simple$scalaSuffix" / "0.2.0-SNAPSHOT"
  }

  test("simple") {
    val baseExpectedArtifacts = Seq(
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
      .toSet

    val expectedSourceEntries = Set(
      "foo/Hello.scala",
      "foo/Messages.scala"
    )

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
      val output = res.out.text().trim
      expect(output == "Hello")

      val sourceJarViaCsStr =
        os.proc(TestUtil.cs, "fetch", repoArgs, "--sources", "--intransitive", dep)
          .call(cwd = root)
          .out.text().trim
      val sourceJarViaCs = os.Path(sourceJarViaCsStr, os.pwd)
      val zf             = new ZipFile(sourceJarViaCs.toIO)
      val entries        = zf.entries().asScala.toVector.map(_.getName).toSet
      expect(entries == expectedSourceEntries)

      val signatures = expectedArtifacts.filter(_.last.endsWith(".asc"))
      assert(signatures.nonEmpty)
      os.proc(
        TestUtil.cli,
        "pgp",
        "verify",
        "--key",
        publicKey,
        signatures.map(os.rel / "test-repo" / TestCase.expectedArtifactsDir / _)
      )
        .call(cwd = root)
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
      Seq(
        os.rel / s"$scalaFile1.scala"           -> s"object $scalaFile1 extends App { println() }",
        os.rel / s"$scalaFile2.scala"           -> s"object $scalaFile2 extends App { println() }",
        os.rel / scriptsDir / s"$scriptName.sc" -> "println()"
      )
    )
    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "publish",
        extraOptions,
        ".",
        "--list-main-classes"
      )
        .call(cwd = root)
      val output = res.out.text().trim
      val resLocal = os.proc(
        TestUtil.cli,
        "publish",
        "local",
        extraOptions,
        ".",
        "--list-main-classes"
      )
        .call(cwd = root)
      val outputLocal = resLocal.out.text().trim
      expect(output == outputLocal)
      val mainClasses = output.linesIterator.toSeq.last.split(" ").toSet
      expect(mainClasses == Set(scalaFile1, scalaFile2, s"$scriptsDir.${scriptName}_sc"))
    }
  }
}
