package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.nio.file.Paths
import java.util.zip.ZipFile

import scala.jdk.CollectionConverters._

class PublishTests extends munit.FunSuite {

  private def extraOptions = TestUtil.extraOptions

  test("simple") {
    val inputs = TestInputs(
      Seq(
        os.rel / "project" / "foo" / "Hello.scala" ->
          """//> using publish.organization "org.virtuslab.scalacli.test"
            |//> using publish.name "simple"
            |//> using publish.version "0.2.0-SNAPSHOT"
            |//> using publish.url "https://github.com/VirtusLab/scala-cli"
            |//> using publish.license "Apache 2.0:http://opensource.org/licenses/Apache-2.0"
            |//> using publish.developer "someone|Someone||https://github.com/someone"
            |
            |//> using scala "3.1.1"
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

    val expectedArtifactsDir =
      os.rel / "org" / "virtuslab" / "scalacli" / "test" / "simple_3" / "0.2.0-SNAPSHOT"
    val baseExpectedArtifacts = Seq(
      "simple_3-0.2.0-SNAPSHOT.pom",
      "simple_3-0.2.0-SNAPSHOT.jar",
      "simple_3-0.2.0-SNAPSHOT-sources.jar"
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
      "--secret-key", secretKey.toString,
      "--secret-key-password", "value:1234",
      "--signer", "bc"
    )
    // format: on

    inputs.fromRoot { root =>
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
      val notInDir = files.filter(!_.startsWith(expectedArtifactsDir))
      expect(notInDir.isEmpty)

      val files0 = files.map(_.relativeTo(expectedArtifactsDir)).toSet

      expect((files0 -- expectedArtifacts).isEmpty)
      expect((expectedArtifacts -- files0).isEmpty)
      expect(files0 == expectedArtifacts) // just in case…

      val repoArgs =
        Seq[os.Shellable]("-r", "!central", "-r", (root / "test-repo").toNIO.toUri.toASCIIString)
      val dep    = "org.virtuslab.scalacli.test:simple_3:0.2.0-SNAPSHOT"
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
        signatures.map(os.rel / "test-repo" / expectedArtifactsDir / _)
      )
        .call(cwd = root)
    }
  }
}
