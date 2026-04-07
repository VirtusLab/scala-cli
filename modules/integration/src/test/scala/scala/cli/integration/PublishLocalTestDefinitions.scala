package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class PublishLocalTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  protected def extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-feature-warning")

  def testedPublishedScalaVersion: String =
    if (actualScalaVersion.startsWith("3"))
      actualScalaVersion.split('.').take(1).mkString
    else actualScalaVersion.split('.').take(2).mkString(".")

  def testPublishVersion: String = "1.5.6"

  protected object PublishTestInputs {
    def testOrg: String  = "test-local-org.sth"
    def testName: String = "my-proj"
    def projFile(
      message: String,
      exclude: Boolean = false,
      useTestScope: Boolean = false,
      crossVersions: Option[Seq[String]] = None
    ): String =
      s"""//> using scala ${crossVersions.map(
          _.mkString(" ")
        ).getOrElse(testedPublishedScalaVersion)}
         |${if (useTestScope) "//> using target.scope test" else ""}
         |//> using dep com.lihaoyi::os-lib:0.11.3${Some(",exclude=com.lihaoyi%%geny").filter(_ =>
          exclude
        ).getOrElse("")}
         |
         |object Project {
         |  def message = "$message"
         |
         |  def main(args: Array[String]): Unit = {
         |    os.pwd
         |    println(message)
         |  }
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

    lazy val projectFilePath: os.RelPath = os.rel / "src" / "project.scala"
    lazy val projectConfPath: os.RelPath = os.rel / "src" / "publish-conf.scala"
    def inputs(
      message: String = "Hello",
      includePublishVersion: Boolean = true,
      excludeGeny: Boolean = false,
      useTestScope: Boolean = false,
      crossVersions: Option[Seq[String]] = None
    ): TestInputs =
      TestInputs(
        projectFilePath -> projFile(message, excludeGeny, useTestScope, crossVersions),
        projectConfPath -> publishConfFile(includePublishVersion)
      )
  }

  for (includePublishVersion <- Seq(true, false)) {
    val withPublishVersionString =
      if (includePublishVersion) " with publish.version"
      else " without explicit publish.version, reading it from git:tag"
    test(s"publish local$withPublishVersionString") {
      val expectedFiles = {
        val modName   = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
        val base      = os.rel / PublishTestInputs.testOrg / modName / testPublishVersion
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
          val ivy2Local  = root / "ivy2" / "local"
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

      val expectedMessage1 = "Hello"
      val expectedMessage2 = "olleH"
      publishLocal()
      val output1 = output()
      expect(output1 == expectedMessage1)

      os.write.over(
        root / PublishTestInputs.projectFilePath,
        PublishTestInputs.projFile(expectedMessage2)
      )
      publishLocal()
      val output2 = output()
      expect(output2 == expectedMessage2)
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

  if (actualScalaVersion.startsWith("3"))
    test("publish local excluding a transitive dependency") {
      PublishTestInputs.inputs(excludeGeny = true).fromRoot { root =>
        val failPublishAsGenyIsntProvided =
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            "local",
            ".",
            extraOptions
          )
            .call(cwd = root, check = false)
        expect(failPublishAsGenyIsntProvided.exitCode == 1)
        val genyDep = "com.lihaoyi::geny:1.1.1"
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--compile-dep",
          genyDep,
          extraOptions
        )
          .call(cwd = root)
        val publishedDep =
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:$testPublishVersion"
        val failRunAsGenyIsntProvided = os.proc(TestUtil.cli, "run", "--dep", publishedDep)
          .call(cwd = root, check = false)
        expect(failRunAsGenyIsntProvided.exitCode == 1)
        os.proc(TestUtil.cli, "run", "--dep", publishedDep, "--dep", genyDep).call(cwd = root)
      }
    }

    test("publish local without docs") {
      val expectedFiles = {
        val modName   = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
        val base      = os.rel / PublishTestInputs.testOrg / modName / testPublishVersion
        val baseFiles = Seq(
          base / "jars" / s"$modName.jar",
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

      PublishTestInputs.inputs()
        .fromRoot { root =>
          os.proc(
            TestUtil.cli,
            "--power",
            "publish",
            "local",
            ".",
            "--ivy2-home",
            os.rel / "ivy2",
            extraOptions,
            "--doc=false"
          )
            .call(cwd = root)
          val ivy2Local  = root / "ivy2" / "local"
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

  test("publish local with test scope") {
    val expectedMessage = "Hello"
    PublishTestInputs.inputs(message = expectedMessage, useTestScope = true).fromRoot { root =>
      val scalaVersionArgs =
        if (actualScalaVersion == Constants.scala3Next)
          Seq("-S", actualScalaVersion)
        else Nil
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "local",
        ".",
        "--test",
        scalaVersionArgs,
        extraOptions
      )
        .call(cwd = root)
      val publishedDep =
        s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:$testPublishVersion"
      val r = os.proc(TestUtil.cli, "run", "--dep", publishedDep, extraOptions).call(cwd = root)
      expect(r.out.trim() == expectedMessage)
    }
  }

  test("publish local --m2") {
    val expectedFiles = {
      val modName = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
      val base    =
        os.rel / PublishTestInputs.testOrg.split('.').toSeq / modName / testPublishVersion
      val baseFiles = Seq(
        base / s"$modName-$testPublishVersion.jar",
        base / s"$modName-$testPublishVersion.pom",
        base / s"$modName-$testPublishVersion-sources.jar",
        base / s"$modName-$testPublishVersion-javadoc.jar"
      )
      baseFiles
        .flatMap { f =>
          val md5  = f / os.up / s"${f.last}.md5"
          val sha1 = f / os.up / s"${f.last}.sha1"
          Seq(f, md5, sha1)
        }
        .toSet
    }

    PublishTestInputs.inputs()
      .fromRoot { root =>
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--m2",
          "--m2-home",
          (root / "m2repo").toString,
          extraOptions
        )
          .call(cwd = root)
        val m2Local    = root / "m2repo"
        val foundFiles = os.walk(m2Local)
          .filter(os.isFile(_))
          .map(_.relativeTo(m2Local))
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

  test("publish local --m2 twice") {
    PublishTestInputs.inputs().fromRoot { root =>
      val m2Repo  = root / "m2repo"
      val modName = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
      val jarPath = m2Repo /
        PublishTestInputs.testOrg.split('.').toSeq /
        modName / testPublishVersion / s"$modName-$testPublishVersion.jar"

      def publishLocal(): os.CommandResult =
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--m2",
          "--m2-home",
          m2Repo.toString,
          "--working-dir",
          os.rel / "work-dir",
          extraOptions
        )
          .call(cwd = root)

      lazy val depsCp: String =
        os.proc(
          TestUtil.cs,
          "fetch",
          "--classpath",
          s"com.lihaoyi:os-lib_$testedPublishedScalaVersion:0.11.3"
        )
          .call(cwd = root)
          .out.trim()

      def output(): String =
        os.proc(
          "java",
          "-cp",
          s"$jarPath${java.io.File.pathSeparator}$depsCp",
          "Project"
        )
          .call(cwd = root)
          .out.trim()

      val expectedMessage1 = "Hello"
      val expectedMessage2 = "olleH"
      publishLocal()
      val output1 = output()
      expect(output1 == expectedMessage1)

      os.write.over(
        root / PublishTestInputs.projectFilePath,
        PublishTestInputs.projFile(expectedMessage2)
      )
      publishLocal()
      val output2 = output()
      expect(output2 == expectedMessage2)
    }
  }

  test("publish local ivy.xml includes license scm and developers") {
    val licenseId  = "MIT"
    val licenseUrl = "https://spdx.org/licenses/MIT.html"
    val vcsOrg     = "it-integ"
    val vcsProj    = "ivy-desc-test"
    val devId      = "itest"
    val devName    = "Integration Tester"
    val devUrl     = "https://itest.example"

    val modName        = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
    val pomProjectName = "Ivy metadata integration display name"

    val scmUrl             = s"https://github.com/$vcsOrg/$vcsProj.git"
    val scmConnection      = s"scm:git:github.com/$vcsOrg/$vcsProj.git"
    val scmDevConnection   = s"scm:git:git@github.com:$vcsOrg/$vcsProj.git"
    val developerDirective = s"$devId|$devName|$devUrl"

    val publishConf =
      s"""//> using publish.organization ${PublishTestInputs.testOrg}
         |//> using publish.moduleName $modName
         |//> using publish.name "$pomProjectName"
         |//> using publish.version $testPublishVersion
         |//> using publish.license $licenseId:$licenseUrl
         |//> using publish.scm github:$vcsOrg/$vcsProj
         |//> using publish.developer "$developerDirective"
         |""".stripMargin

    TestInputs(
      PublishTestInputs.projectFilePath -> PublishTestInputs.projFile("Hello"),
      PublishTestInputs.projectConfPath -> publishConf
    ).fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "--power",
        "publish",
        "local",
        ".",
        "--ivy2-home",
        os.rel / "ivy2",
        extraOptions
      )
        .call(cwd = root)

      val ivyPath = root / "ivy2" / "local" / PublishTestInputs.testOrg / modName /
        testPublishVersion / "ivys" / "ivy.xml"
      val pomPath = root / "ivy2" / "local" / PublishTestInputs.testOrg / modName /
        testPublishVersion / "poms" / s"$modName.pom"
      expect(os.exists(ivyPath))
      expect(os.exists(pomPath))
      val ivyXml = os.read(ivyPath)
      val pomXml = os.read(pomPath)

      expect(ivyXml.contains(s"""<license name="$licenseId""""))
      expect(ivyXml.contains(s"""url="$licenseUrl""""))

      expect(ivyXml.contains("""xmlns:m="http://maven.apache.org/POM/4.0.0""""))
      expect(ivyXml.contains(s"<m:name>$pomProjectName</m:name>"))
      expect(ivyXml.contains("<m:scm>"))
      expect(ivyXml.contains("<m:developers>"))

      expect(ivyXml.contains(s"<m:url>$scmUrl</m:url>"))
      expect(ivyXml.contains(s"<m:connection>$scmConnection</m:connection>"))
      expect(ivyXml.contains(s"<m:developerConnection>$scmDevConnection</m:developerConnection>"))

      expect(ivyXml.contains(s"<m:id>$devId</m:id>"))
      expect(ivyXml.contains(s"<m:name>$devName</m:name>"))
      expect(ivyXml.contains(s"<m:url>$devUrl</m:url>"))

      expect(pomXml.contains(s"<name>$pomProjectName</name>"))
    }
  }

  if actualScalaVersion.startsWith("3") then
    test("publish local with compileOnly.dep") {
      TestInputs(
        os.rel / "project.scala" ->
          s"""//> using compileOnly.dep org.springframework.boot:spring-boot:3.5.6
             |//> using test.dep org.springframework.boot:spring-boot:3.5.6
             |
             |//> using publish.organization my.org
             |//> using publish.name scala-cli-publish-bug
             |//> using publish.version 1.0.0
             |""".stripMargin,
        os.rel / "RootLoggerConfigurer.scala" ->
          s"""import org.springframework.beans.factory.annotation.Autowired
             |import scala.compiletime.uninitialized
             |
             |class RootLoggerConfigurer:
             |  @Autowired var sentryClient: String = uninitialized
             |""".stripMargin
      ).fromRoot { root =>
        val scalaVersionArgs =
          if (actualScalaVersion == Constants.scala3Next)
            Seq("-S", actualScalaVersion)
          else Nil
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          scalaVersionArgs,
          extraOptions
        )
          .call(cwd = root)
      }
    }
}
