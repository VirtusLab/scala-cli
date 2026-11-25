package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.nio.file.Paths

import scala.cli.integration.TestUtil.stableStdoutOf
import scala.util.Properties

class PublishTestsDefault extends PublishTestDefinitions with TestDefault {

  /** The directory of the `gpg` executable, if any
    *
    * Windows CI runners don't have `gpg` on their `PATH`, but Git for Windows comes with one.
    */
  private lazy val gpgDirOpt: Option[os.Path] = {
    val executable = if Properties.isWin then "gpg.exe" else "gpg"
    val pathDirs   = Option(System.getenv("PATH")).toSeq
      .flatMap(_.split(File.pathSeparator))
      .filter(_.nonEmpty)
      .flatMap(dir => scala.util.Try(os.Path(dir, os.pwd)).toOption)
    val gitForWindowsDirs =
      if Properties.isWin then
        Seq("ProgramFiles", "ProgramW6432")
          .flatMap(v => Option(System.getenv(v)))
          .map(dir => os.Path(dir) / "Git" / "usr" / "bin")
      else Nil
    (pathDirs ++ gitForWindowsDirs).find(dir => os.isFile(dir / executable))
  }

  /** `path`, as the `gpg` in `gpgDir` understands it
    *
    * MSYS2 programs, like the `gpg` of Git for Windows, take `/c/…` paths as absolute, but not
    * `C:\…` ones.
    */
  private def gpgPath(gpgDir: os.Path, path: os.Path): String =
    if os.isFile(gpgDir / "msys-2.0.dll") then
      path.toString.replace('\\', '/') match {
        case s"$drive:/$rest" if drive.length == 1 => s"/${drive.toLowerCase}/$rest"
        case other                                 => other
      }
    else path.toString

  test("sign with gpg") {
    assume(gpgDirOpt.isDefined, "gpg not found")
    val gpgDir = gpgDirOpt.get
    TestCase.testInputs().fromRoot { root =>
      // outside of root, as gpg-agent sockets live there, and their paths can't be too long
      val gnupgHome = os.temp.dir(prefix = "scala-cli-gpg", deleteOnExit = false)
      if !Properties.isWin then os.perms.set(gnupgHome, "rwx------")
      val env = Map(
        "GNUPGHOME" -> gpgPath(gpgDir, gnupgHome),
        "PATH"      -> (gpgDir.toString + File.pathSeparator + System.getenv("PATH"))
      )
      def gpg(args: os.Shellable*): os.CommandResult =
        os.proc(gpgDir / (if Properties.isWin then "gpg.exe" else "gpg"), "--batch", args)
          .call(cwd = root, env = env, mergeErrIntoOut = true)

      try {
        gpg(
          "--pinentry-mode",
          "loopback",
          "--passphrase",
          "",
          "--quick-generate-key",
          "Scala CLI Tests <scala-cli-tests@example.com>",
          "default",
          "sign",
          "never"
        )
        val keyId = gpg("--list-secret-keys", "--with-colons").out.lines()
          .collectFirst { case line if line.startsWith("fpr:") => line.split(':')(9) }
          .getOrElse(sys.error("No GPG key generated"))

        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          extraOptions,
          "--signer",
          "gpg",
          "--gpg-key",
          keyId,
          "--gpg-option",
          "--batch",
          "project",
          "-R",
          "test-repo"
        ).call(cwd = root, env = env, stdout = os.Inherit)

        val artifactsDir  = root / "test-repo" / TestCase.expectedArtifactsDir
        val files         = os.list(artifactsDir).map(_.last).toSet
        val expectedFiles = expectedArtifacts.map(_.last)
        expect(files == expectedFiles)

        for (signature <- expectedFiles.filter(_.endsWith(".asc"))) {
          val res =
            gpg("--verify", artifactsDir / signature, artifactsDir / signature.stripSuffix(".asc"))
          expect(res.exitCode == 0)
        }
      }
      finally {
        // gpg-agent keeps files open in GNUPGHOME, which prevents deleting it on Windows
        val gpgconf = gpgDir / (if Properties.isWin then "gpgconf.exe" else "gpgconf")
        if os.isFile(gpgconf) then
          os.proc(gpgconf, "--kill", "all").call(cwd = root, env = env, check = false)
        scala.util.Try(os.remove.all(gnupgHome))
      }
    }
  }
  test("Pure Java") {
    val testOrg     = "test-org.foo"
    val testName    = "foo"
    val testVersion = "0.3.1"
    val inputs      = TestInputs(
      os.rel / "Foo.java" ->
        s"""//> using publish.organization $testOrg
           |//> using publish.name $testName
           |//> using publish.version $testVersion
           |
           |package foo;
           |
           |public class Foo {
           |  private static boolean checkClass(String clsName) {
           |    try {
           |      Thread.currentThread().getContextClassLoader().loadClass(clsName);
           |      return true;
           |    } catch (ClassNotFoundException ex) {
           |      return false;
           |    }
           |  }
           |
           |  public static void main(String[] args) {
           |    boolean hasJuList = checkClass("java.util.List");
           |    boolean hasScalaArray = checkClass("scala.Array");
           |    if (!hasJuList) {
           |      System.out.println("Error: java.util.List not found");
           |      System.exit(1);
           |    }
           |    if (hasScalaArray) {
           |      System.out.println("Error: unexpectedly found scala.Array");
           |      System.exit(1);
           |    }
           |    System.out.println("Hello from " + "foo");
           |  }
           |}
           |""".stripMargin
    )

    val repoRelPath = os.rel / "test-repo"
    inputs.fromRoot { root =>
      os.proc(TestUtil.cli, "--power", "publish", extraOptions, ".", "-R", repoRelPath)
        .call(stdin = os.Inherit, stdout = os.Inherit, cwd = root)
      val repoRoot = root / repoRelPath
      val baseDir  = repoRoot / testOrg.split('.').toSeq / testName / testVersion
      expect(os.isDir(baseDir))

      val res = stableStdoutOf(
        TestUtil.cs,
        "launch",
        s"$testOrg:$testName:$testVersion",
        "-r",
        repoRoot.toNIO.toUri.toASCIIString
      )(stdin = os.Inherit, cwd = root)
      val output = res
      expect(output == "Hello from foo")
    }
  }

  test("scalapy") {

    def maybeScalapyPrefix =
      if (actualScalaVersion.startsWith("2.13.")) ""
      else "import me.shadaj.scalapy.py" + System.lineSeparator()

    val sbv =
      if (actualScalaVersion.startsWith("3.")) "3"
      else actualScalaVersion.split('.').take(2).mkString(".")

    val org  = "test-org"
    val name = "test-name"
    val ver  = "0.3.6"

    val inputs = TestInputs(
      os.rel / "src" / "Hello.scala" ->
        s"""$maybeScalapyPrefix
           |object Hello {
           |  def main(args: Array[String]): Unit = {
           |    py.Dynamic.global.print("Hello from Python", flush = true)
           |  }
           |}
           |""".stripMargin
    )

    val publishArgs = Seq(
      "--organization",
      org,
      "--name",
      name,
      "--project-version",
      ver
    )

    inputs.fromRoot { root =>
      val repoRoot = root / "tmp-repo"
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "--python",
        "--publish-repo",
        repoRoot,
        "src",
        publishArgs
      )
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
      val res = stableStdoutOf(
        TestUtil.cs,
        "launch",
        "--python",
        "--no-default",
        "-r",
        "central",
        "-r",
        repoRoot.toNIO.toUri.toASCIIString,
        s"$org:${name}_$sbv:$ver"
      )(cwd = root)
      val output = res
      expect(output == "Hello from Python")
    }
  }

  test("missing org and version") {
    // Missing org and missing version should be reported at the same time,
    // rather than one at a time.
    val inputs = TestInputs(
      os.rel / "Messages.scala" ->
        """package messages
          |
          |object Messages {
          |  def hello = "Hello"
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val tmpDir = os.temp.dir(prefix = "scala-cli-publish-test")
      for (f <- os.list(root))
        os.copy.into(f, tmpDir)
      val publishRepo = root / "the-repo"
      val res = os.proc(TestUtil.cli, "--power", "publish", "--publish-repo", publishRepo, tmpDir)
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      val output = res.out.text()
      expect(output.contains("Missing organization"))
      expect(output.contains("Missing version"))
    }
  }

  test("missing sonatype requirements") {
    val inputs = TestInputs(
      os.rel / "messages" / "Messages.scala" ->
        """//> using publish.repository central
          |//> using publish.organization test-org
          |//> using publish.name test-name
          |//> using publish.version 0.1.0
          |package messages
          |object Messages {
          |  def hello = "Hello"
          |}
          |""".stripMargin,
      os.rel / "publish-conf.scala" ->
        """//> using publish.url https://github.com/me/my-project
          |//> using publish.license Apache-2.0
          |//> using publish.scm github:test-org/test-name
          |//> using publish.developer me|Me|https://me.me
          |""".stripMargin
    )
    def checkWarnings(output: String, hasWarnings: Boolean): Unit = {
      val lines = Seq(
        "Missing project URL",
        "Missing license",
        "Missing SCM information",
        "Missing developer"
      )
      for (line <- lines)
        if (hasWarnings)
          expect(output.contains(line))
        else
          expect(!output.contains(line))
    }
    def checkCredentialsWarning(output: String): Unit =
      expect(
        output.contains(
          "Publishing to a repository that needs authentication, but no credentials are available."
        )
      )
    inputs.fromRoot { root =>
      val failRes =
        os.proc(TestUtil.cli, "--power", "publish", "--dummy", "--signer", "none", "messages")
          .call(cwd = root, mergeErrIntoOut = true)
      checkWarnings(failRes.out.text(), hasWarnings = true)
      checkCredentialsWarning(failRes.out.text())

      val okRes = os.proc(TestUtil.cli, "--power", "publish", "--dummy", "--signer", "none", ".")
        .call(cwd = root, mergeErrIntoOut = true)
      checkWarnings(okRes.out.text(), hasWarnings = false)
      checkCredentialsWarning(okRes.out.text())
    }
  }

  test(s"simple failed upload") {
    val secretKey = {
      val uri = Thread.currentThread().getContextClassLoader
        .getResource("test-keys/key.skr")
        .toURI
      os.Path(Paths.get(uri))
    }

    val signingOptions = Seq(
      "--secret-key",
      s"file:$secretKey",
      "--secret-key-password",
      "value:1234",
      "--signer",
      "bc"
    )

    TestCase.testInputs().fromRoot { root =>
      val r = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        signingOptions,
        "project",
        "--publish-repository",
        "sonatype:central"
      ).call(
        cwd = root,
        stdin = os.Inherit,
        stdout = os.Inherit,
        check = false
      )
      expect(r.exitCode != 0)
    }
  }
}
