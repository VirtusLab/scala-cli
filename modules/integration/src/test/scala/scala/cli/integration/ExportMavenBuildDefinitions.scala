package scala.cli.integration

abstract class ExportMavenTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs with ExportCommonTestDefinitions with MavenTestHelper {
  _: TestScalaVersion =>
  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "--power",
      "export",
      extraOptions,
      "--maven",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, args: String*): os.proc = mavenCommand(args*)

  override val runMainArgs: Seq[String]  = Seq("run")
  override val runTestsArgs: Seq[String] = Seq("test")

}
