package scala.cli.integration

class ExportMavenTests212 extends ExportMavenTestDefinitions with Test212 {
  test("pure java") {
    simpleTest(ExportTestProjects.pureJavaTest)
  }
}
