package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File

trait PublishSlothTestDefinitions { this: PublishTestDefinitions & TestScalaVersion =>
  if actualScalaVersion.startsWith("3.") then {
    val latestJava             = Constants.allJavaVersions.max
    val publishScalaVersion    = Constants.scala3Lts
    val expectedMessage        = "Hello"
    val slothNoOpWarnPrefix    = "Sloth patching is not applicable to"
    val slothAgentWarnFragment = "is not applicable to publish"
    val testOrg                = "test-publish-sloth-org"
    val testName               = "sloth-lib"
    val testVersion            = "0.1.0"
    val dep                    = s"$testOrg:${testName}_3:$testVersion"
    val slothOptions           = Seq("--sloth", "--suppress-experimental-feature-warning")
    val slothAgentOptions      = Seq("--sloth-agent", "--suppress-experimental-feature-warning")
    val slothCacheSegment      = s"${File.separator}sloth${File.separator}"

    def expectScaladocClasspathContains(output: String, fragment: String): Unit = {
      val marker       = "dotty.tools.scaladoc.Main -classpath "
      val classpathOpt = output.split(marker).lift(1).map(_.takeWhile(c => c != ' ' && c != '\n'))
      expect(classpathOpt.exists(_.contains(fragment)))
    }

    def lazyValProjFile: String =
      s"""//> using scala $publishScalaVersion
         |//> using publish.organization $testOrg
         |//> using publish.name $testName
         |//> using publish.version $testVersion
         |
         |object Main {
         |  lazy val greeting: String = "$expectedMessage"
         |  def main(args: Array[String]): Unit = println(greeting)
         |}
         |""".stripMargin

    def publishToRepo(
      root: os.Path,
      extraArgs: Seq[String],
      repo: os.Path,
      mergeErrIntoOut: Boolean = false
    ): os.CommandResult =
      val baseCall = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        extraOptions,
        extraArgs,
        ".",
        "--publish-repo",
        repo
      )
      if mergeErrIntoOut then baseCall.call(cwd = root, mergeErrIntoOut = true)
      else baseCall.call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

    test(s"publish --sloth patches lazy vals on JDK $latestJava") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val repo = root / "test-repo"
        publishToRepo(root, slothOptions, repo)
        val r = os.proc(
          TestUtil.cli,
          "run",
          extraOptions,
          "--dep",
          dep,
          "-M",
          "Main",
          "--jvm",
          latestJava.toString,
          "-r",
          repo.toNIO.toUri.toASCIIString
        ).call(cwd = root, stderr = os.Pipe)
        expect(r.out.trim().contains(expectedMessage))
        expect(!r.err.trim().contains("sun.misc.Unsafe"))
      }
    }

    for warningKeyword <- Seq("source jars") do
      test(s"publish --sloth warns that sloth is not applicable to $warningKeyword") {
        TestInputs(
          os.rel / "Main.scala" -> lazyValProjFile
        ).fromRoot { root =>
          val repo = root / "test-repo"
          val r    = publishToRepo(root, slothOptions, repo, mergeErrIntoOut = true)
          expect(r.out.trim().contains(slothNoOpWarnPrefix))
          expect(r.out.trim().contains(warningKeyword))
        }
      }

    test("publish --sloth patches the doc-generation classpath") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val repo = root / "test-repo"
        val r    = publishToRepo(root, slothOptions ++ Seq("-v"), repo, mergeErrIntoOut = true)
        expect(r.exitCode == 0)
        expectScaladocClasspathContains(r.out.text(), slothCacheSegment)
      }
    }

    test("publish --sloth-agent is rejected with a warning") {
      TestInputs(
        os.rel / "Main.scala" -> lazyValProjFile
      ).fromRoot { root =>
        val repo = root / "test-repo"
        val r    = publishToRepo(root, slothAgentOptions, repo, mergeErrIntoOut = true)
        expect(r.exitCode == 0)
        expect(r.out.trim().contains(slothAgentWarnFragment))
      }
    }
  }
}
