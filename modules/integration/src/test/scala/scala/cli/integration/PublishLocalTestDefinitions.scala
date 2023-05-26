package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class PublishLocalTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {
  protected def extraOptions: Seq[String] =
    scalaVersionArgs ++ TestUtil.extraOptions ++ Seq("--suppress-experimental-feature-warning")

  def testedPublishedScalaVersion: String =
    if (actualScalaVersion.startsWith("3"))
      actualScalaVersion.split('.').take(1).mkString
    else actualScalaVersion.split('.').take(2).mkString(".")

  private object PublishTestInputs {
    def testOrg: String     = "test-local-org.sth"
    def testName: String    = "my-proj"
    def testVersion: String = "1.5.6"
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

    private val publishConfFile: String =
      s"""//> using publish.organization $testOrg
         |//> using publish.name $testName
         |//> using publish.version $testVersion
         |""".stripMargin

    val inputs: TestInputs = TestInputs(
      os.rel / "project.scala"      -> projFile("Hello"),
      os.rel / "publish-conf.scala" -> publishConfFile
    )
  }

  test("publish local") {
    val expectedFiles = {
      val modName = s"${PublishTestInputs.testName}_$testedPublishedScalaVersion"
      val base    = os.rel / PublishTestInputs.testOrg / modName / PublishTestInputs.testVersion
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

    PublishTestInputs.inputs.fromRoot { root =>
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

  test("publish local twice") {
    PublishTestInputs.inputs.fromRoot { root =>
      def publishLocal(): os.CommandResult =
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          "--ivy2-home",
          os.rel / "ivy2",
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
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$testedPublishedScalaVersion:${PublishTestInputs.testVersion}"
        )
          .call(cwd = root)
          .out.trim()

      publishLocal()
      val output1 = output()
      expect(output1 == "Hello")

      os.write.over(root / "Project.scala", PublishTestInputs.projFile("olleH"))
      publishLocal()
      val output2 = output()
      expect(output2 == "olleH")
    }
  }

}
