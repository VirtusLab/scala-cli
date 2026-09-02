package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait PublishSlothTestDefinitions extends LazyValTests:
  this: PublishTestDefinitions & TestScalaVersion =>

  if actualScalaVersion.startsWith("3.") then
    val latestJava             = Constants.allJavaVersions.max
    val expectedMessage        = "Hello"
    val slothAgentWarnFragment = "is not applicable to publish"
    val testOrg                = "test-publish-sloth-org"
    val testName               = "sloth-lib"
    val testVersion            = "0.1.0"
    val dep                    = s"$testOrg:${testName}_3:$testVersion"

    def lazyValProjFile: String =
      s"""//> using publish.organization $testOrg
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
      mergeErrIntoOut: Boolean = false,
      scalaVersionOverride: String = actualScalaVersion
    ): os.CommandResult = {
      val baseCall = os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        TestUtil.extraOptions,
        "-S",
        scalaVersionOverride,
        extraArgs,
        ".",
        "--publish-repo",
        repo
      )
      if mergeErrIntoOut then baseCall.call(cwd = root, mergeErrIntoOut = true)
      else baseCall.call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)
    }

    if !isScala38OrNewer then {
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
    }

    test(
      s"publish reflects --sloth toggle for ${Constants.scala3LegacyLts} lazy vals on JDK $latestJava"
    ) {
      val ltsProjFile =
        s"""//> using publish.organization $testOrg
           |//> using publish.name $testName
           |//> using publish.version $testVersion
           |
           |object Main {
           |  lazy val greeting: String = "$expectedMessage"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin
      TestInputs(
        os.rel / "Main.scala" -> ltsProjFile
      ).fromRoot { root =>
        def runPublished(repo: os.Path): os.CommandResult =
          os.proc(
            TestUtil.cli,
            "run",
            "--dep",
            dep,
            "-M",
            "Main",
            "--jvm",
            latestJava.toString,
            "-r",
            repo.toNIO.toUri.toASCIIString,
            extraOptions
          ).call(cwd = root, stderr = os.Pipe)

        val withSlothRepo = root / "test-repo-sloth"
        publishToRepo(
          root,
          slothOptions,
          withSlothRepo,
          scalaVersionOverride = Constants.scala3LegacyLts
        )
        val withSloth = runPublished(withSlothRepo)
        expect(withSloth.out.trim().contains(expectedMessage))
        expect(!withSloth.err.trim().contains("sun.misc.Unsafe"))

        val withoutSlothRepo = root / "test-repo-no-sloth"
        publishToRepo(root, Nil, withoutSlothRepo, scalaVersionOverride = Constants.scala3LegacyLts)
        val withoutSloth = runPublished(withoutSlothRepo)
        expect(withoutSloth.out.trim().contains(expectedMessage))
        expect(withoutSloth.err.trim().contains("sun.misc.Unsafe"))
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

    test("publish --sloth preserves user META-INF/MANIFEST.MF from resources") {
      val projFile =
        s"""//> using scala ${Constants.scala3LegacyLts}
           |//> using resourceDir resources
           |//> using publish.organization $testOrg
           |//> using publish.name $testName
           |//> using publish.version $testVersion
           |
           |object Main {
           |  lazy val greeting: String = "$expectedMessage"
           |  def main(args: Array[String]): Unit = println(greeting)
           |}
           |""".stripMargin
      TestInputs(
        os.rel / "Main.scala"                             -> projFile,
        os.rel / "resources" / "META-INF" / "MANIFEST.MF" -> userManifestResourceContent
      ).fromRoot { root =>
        val repo = root / "test-repo"
        publishToRepo(root, slothOptions, repo)
        val publishedJar = os.walk(repo)
          .find(p =>
            p.last.endsWith(".jar") && !p.last.contains("-sources") && !p.last.contains("-javadoc")
          )
          .getOrElse(sys.error(s"Published jar not found under $repo"))
        val attrs = jarManifestMainAttributes(publishedJar)
        expect(attrs.get("X-Custom").contains("yes"))
        expect(attrs.get("Main-Class").contains("Main"))
      }
    }
