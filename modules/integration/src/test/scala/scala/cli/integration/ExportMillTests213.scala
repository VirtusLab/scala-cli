package scala.cli.integration

class ExportMillTests213 extends ExportMillTestDefinitions with Test213 {
  if (runExportTests) {
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion))
    }
    test("pure java") {
      simpleTest(ExportTestProjects.pureJavaTest)
    }
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion))
    }
  }

  override lazy val majorScalaVersion: String = "2.13"
}
