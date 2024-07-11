package scala.cli.integration

class ExportMavenTests212 extends ExportMavenTestDefinitions with Test212 with MavenScala {
  test("pure java") {
    simpleTest(ExportTestProjects.pureJavaTest)
  }
}
