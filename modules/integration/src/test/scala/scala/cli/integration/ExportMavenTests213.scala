package scala.cli.integration

class ExportMavenTests213 extends ExportMavenTestDefinitions with Test213 {
  test("pure java") {
    simpleTest(ExportTestProjects.pureJavaTest)
  }
}
