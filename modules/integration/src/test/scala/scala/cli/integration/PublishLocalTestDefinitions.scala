package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class PublishLocalTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  protected def extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-feature-warning")

  def testedPublishedScalaVersion: String =
    if (actualScalaVersion.startsWith("3"))
      actualScalaVersion.split('.').take(1).mkString
    else actualScalaVersion.split('.').take(2).mkString(".")

  def testPublishVersion: String = "1.5.6"

  private object PublishTestInputs {
    def testOrg: String  = "test-local-org.sth"
    def testName: String = "my-proj"
    def projFile(message: String): String =
      s"""//> using scala "$testedPublishedScalaVersion"
         |//> using dep "com.lihaoyi::os-lib:0.9.1"
         |
         |object Project {
         |  def message = "$message"
         |
         |  def main(args: Array[String]): Unit =
         |    println(message)
         |}
         |""".stripMargin

    private def publishConfFile(includePublishVersion: Boolean): String = {
      val publishVersionDirective =
        if (includePublishVersion) s"//> using publish.version $testPublishVersion"
        else ""
      s"""//> using publish.organization $testOrg
         |//> using publish.name $testName
         |$publishVersionDirective
         |""".stripMargin
    }

    def inputs(message: String = "Hello", includePublishVersion: Boolean = true): TestInputs =
      TestInputs(
        os.rel / "project.scala"      -> projFile(message),
        os.rel / "publish-conf.scala" -> publishConfFile(includePublishVersion)
      )
  }

  for (includePublishVersion <- Seq(true, false)) {
    val withPublishVersionString =
      if (includePublishVersion) " with publish.version"
      else " without explicit publish.version, reading it from git:tag"
    test(s"publish local$withPublishVersionString") {
      val expectedFiles = {
        val modName = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
        val base    = os.rel / PublishTestInputs.testOrg / modName / testPublishVersion
        val baseFiles = Seq(
          base / "jars" / s"$modName.jar",
          base / "docs" / s"$modName-javadoc.jar",
          base / "srcs" / s"$modName-sources.jar",
          base / "poms" / s"$modName.pom",
          base / "ivys" / "ivy.xml"
        )
        baseFiles
          .flatMap { f =>
            val md5  = f / os.up / s"${f.last}.md5"
            val sha1 = f / os.up / s"${f.last}.sha1"
            Seq(f, md5, sha1)
          }
          .toSet
      }

      PublishTestInputs.inputs(includePublishVersion = includePublishVersion)
        .fromRoot { root =>
          val ciOptions =
            if (!includePublishVersion) {
              TestUtil.initializeGit(cwd = root, tag = testPublishVersion)
              Seq("--ci=false") // when running on CI, version wouldn't be read from a git tag
            }
            else Seq.empty
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            "local",
            ".",
            "--ivy2-home",
            os.rel / "ivy2",
            extraOptions,
            ciOptions
          )
            .call(cwd = root)
          val ivy2Local = root / "ivy2" / "local"
          val foundFiles = os.walk(ivy2Local)
            .filter(os.isFile(_))
            .map(_.relativeTo(ivy2Local))
            .toSet
          val missingFiles    = expectedFiles -- foundFiles
          val unexpectedFiles = foundFiles -- expectedFiles
          if (missingFiles.nonEmpty)
            pprint.err.log(missingFiles)
          if (unexpectedFiles.nonEmpty)
            pprint.err.log(unexpectedFiles)
          expect(missingFiles.isEmpty)
          expect(unexpectedFiles.isEmpty)
        }
    }
  }

  test("publish local twice") {
    PublishTestInputs.inputs().fromRoot { root =>
      def publishLocal(): os.CommandResult =
        os.proc(
          TestUtil.cli,
          "publish",
          "local",
          ".",
          "--ivy2-home",
          os.rel / "ivy2",
          "--power", // Test --power placed after subcommand name
          "--working-dir",
          os.rel / "work-dir",
          extraOptions
        )
          .call(cwd = root)

      def output(): String =
        os.proc(
          TestUtil.cs,
          s"-J-Divy.home=${root / "ivy2"}",
          "launch",
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:$testPublishVersion"
        )
          .call(cwd = root)
          .out.trim()

      publishLocal()
      val output1 = output()
      expect(output1 == "Hello")

      os.write.over(root / "project.scala", PublishTestInputs.projFile("olleH"))
      publishLocal()
      val output2 = output()
      expect(output2 == "olleH")
    }
  }

  test("publish local with ivy.home") {
    PublishTestInputs.inputs().fromRoot { root =>
      def publishLocal(): os.CommandResult =
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--working-dir",
          os.rel / "work-dir",
          extraOptions
        )
          .call(
            cwd = root,
            env = Map(
              "JAVA_OPTS" -> s"-Divy.home=${root / "ivyhome"} -Duser.home${root / "userhome"}"
            ) // ivy.home takes precedence
          )

      def output(): String =
        os.proc(
          TestUtil.cs,
          s"-J-Divy.home=${root / "ivyhome"}",
          "launch",
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:$testPublishVersion"
        )
          .call(cwd = root)
          .out.trim()

      publishLocal()
      expect(output() == "Hello")
    }
  }

  test("publish local with user.home") {
    PublishTestInputs.inputs().fromRoot { root =>
      def publishLocal(): os.CommandResult =
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--working-dir",
          os.rel / "work-dir",
          extraOptions
        )
          .call(
            cwd = root,
            env = Map(
              "JAVA_OPTS" -> s"-Duser.home=${root / "userhome"}"
            )
          )

      def output(): String =
        os.proc(
          TestUtil.cs,
          s"-J-Divy.home=${root / "userhome" / ".ivy2"}",
          "launch",
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:$testPublishVersion"
        )
          .call(cwd = root)
          .out.trim()

      publishLocal()
      expect(output() == "Hello")
    }
  }

}
