package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class PublishTestsDefault extends PublishTestDefinitions(scalaVersionOpt = None) {
  test("Pure Java") {
    val testOrg     = "test-org.foo"
    val testName    = "foo"
    val testVersion = "0.3.1"
    val inputs = TestInputs(
      os.rel / "Foo.java" ->
        s"""//> using publish.organization "$testOrg"
           |//> using publish.name "$testName"
           |//> using publish.version "$testVersion"
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

      val res = os.proc(
        TestUtil.cs,
        "launch",
        s"$testOrg:$testName:$testVersion",
        "-r",
        repoRoot.toNIO.toUri.toASCIIString
      )
        .call(stdin = os.Inherit, cwd = root)
      val output = res.out.trim()
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
      val res = os.proc(
        TestUtil.cs,
        "launch",
        "--python",
        "--no-default",
        "-r",
        "central",
        "-r",
        repoRoot.toNIO.toUri.toASCIIString,
        s"$org:${name}_$sbv:$ver"
      ).call(cwd = root)
      val output = res.out.trim()
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
        """//> using publish.repository "central"
          |//> using publish.organization "test-org"
          |//> using publish.name "test-name"
          |//> using publish.version "0.1.0"
          |package messages
          |object Messages {
          |  def hello = "Hello"
          |}
          |""".stripMargin,
      os.rel / "publish-conf.scala" ->
        """//> using publish.url "https://github.com/me/my-project"
          |//> using publish.license "Apache-2.0"
          |//> using publish.scm "github:test-org/test-name"
          |//> using publish.developer "me|Me|https://me.me"
          |""".stripMargin
    )
    def checkWarnings(output: String, hasWarnings: Boolean): Unit = {
      val lines = Seq(
        "project URL is empty",
        "license is empty",
        "SCM details are empty",
        "developer details are empty"
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
}
