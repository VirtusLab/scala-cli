package scala.cli.integration

class ExportMillTests213 extends ExportMillTestDefinitions with Test213 {
  if (runExportTests) {
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion), mainClass = None)
    }
    test("pure java") {
      simpleTest(ExportTestProjects.pureJavaTest("ScalaCliJavaTest"), mainClass = None)
    }
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion), mainClass = None)
    }
  }
}
