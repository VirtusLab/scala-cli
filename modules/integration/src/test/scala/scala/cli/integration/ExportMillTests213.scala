package scala.cli.integration

// format: off
class ExportMillTests213 extends ExportMillTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
  // format: on

  if (runExportTests)
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion))
    }

  if (runExportTests)
    test("pure java") {
      simpleTest(ExportTestProjects.pureJavaTest)
    }

  if (runExportTests)
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion))
    }

}
