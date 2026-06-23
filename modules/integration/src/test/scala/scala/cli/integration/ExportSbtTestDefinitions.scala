package scala.cli.integration

abstract class ExportSbtTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs
    with ExportCommonTestDefinitions
    with ExportScalaOrientedBuildToolsTestDefinitions
    with SbtTestHelper { this: TestScalaVersion & TestSbtVersion =>
  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "--power",
      "export",
      extraOptions,
      "--sbt",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, mainClass: Option[String], args: String*): os.proc =
    sbtCommand(sbtVersion, args*)

  override def runMainArgs(mainClass: Option[String]): Seq[String]  = Seq("run")
  override def runTestsArgs(mainClass: Option[String]): Seq[String] =
    if sbtVersion.startsWith("2.") then Seq("testFull") else Seq("test")

  override def commonTestDescriptionSuffix: String =
    s" (Sbt $sbtVersion & Scala $actualScalaVersion)"

  override protected def defaultExportCommandArgs: Seq[String] = Seq("--sbt-version", sbtVersion)

  if runScalaNativeExportTest then
    test(s"Scala Native$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        simpleTest(
          ExportTestProjects.nativeTest(actualScalaVersion),
          mainClass = None,
          extraExportArgs = defaultExportCommandArgs
        )
      }
    }

}

sealed trait TestSbtVersion:
  def sbtVersion: String

trait TestSbt1 extends TestSbtVersion:
  self: ExportSbtTestDefinitions =>
  override def sbtVersion: String = Constants.sbt1Version

trait TestSbt2 extends TestSbtVersion:
  self: ExportSbtTestDefinitions =>
  override def sbtVersion: String                          = Constants.sbt2Version
  override protected def runScalaJsExportTest: Boolean     = false // TODO: enable when available
  override protected def runScalaNativeExportTest: Boolean = false // TODO: enable when available
