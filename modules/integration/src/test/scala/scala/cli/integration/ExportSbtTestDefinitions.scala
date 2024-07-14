package scala.cli.integration

abstract class ExportSbtTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs with ExportCommonTestDefinitions
    with ExportScalaOrientedBuildToolsTestDefinitions with SbtTestHelper {
  _: TestScalaVersion =>
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

  override def buildToolCommand(root: os.Path, args: String*): os.proc = sbtCommand(args*)

  override val runMainArgs: Seq[String]  = Seq("run")
  override val runTestsArgs: Seq[String] = Seq("test")

  test("Scala Native") {
    simpleTest(ExportTestProjects.nativeTest(actualScalaVersion))
  }
}
