package scala.cli.integration

class ExportMavenTestJava extends ExportMavenTestDefinitions with Test3Lts with MavenJava {
  // disable running scala tests in java maven export
  override def runExportTests: Boolean = false
  test("pure java") {
    simpleTest(
      ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
      mainClass = Some("ScalaCliJavaTest")
    )
  }
}
