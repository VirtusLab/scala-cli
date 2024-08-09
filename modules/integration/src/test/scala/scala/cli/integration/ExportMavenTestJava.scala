package scala.cli.integration

import scala.util.Properties

class ExportMavenTestJava extends ExportMavenTestDefinitions with Test3Lts with MavenJava {
  // disable running scala tests in java maven export
  override def runExportTests: Boolean = false
  if (!Properties.isWin) test("pure java") {
    simpleTest(
      ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
      mainClass = Some("ScalaCliJavaTest")
    )
  }
}
