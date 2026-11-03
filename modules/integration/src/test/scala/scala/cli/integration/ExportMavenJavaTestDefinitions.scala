package scala.cli.integration

import scala.util.Properties

trait ExportMavenJavaTestDefinitions extends ExportCommonTestDefinitions with MavenJava {
  this: ExportMavenTestDefinitions =>
  // disable running scala tests in java maven export
  override def runExportTests: Boolean = false
  if (!Properties.isWin) test("pure java") {
    simpleTest(
      ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
      mainClass = Some("ScalaCliJavaTest")
    )
  }
}
