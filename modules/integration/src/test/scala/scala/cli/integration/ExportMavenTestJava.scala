package scala.cli.integration

class ExportMavenTestJava extends ExportMavenTestDefinitions with Test3Lts with MavenJava {
  test("pure java") {
    simpleTest(ExportTestProjects.pureJavaTest("ScalaCliJavaTest"), mainClass = Some("ScalaCliJavaTest"))
  }
}
