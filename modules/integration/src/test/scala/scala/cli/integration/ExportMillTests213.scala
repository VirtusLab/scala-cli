package scala.cli.integration

// format: off
class ExportMillTests213 extends ExportMillTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
  // format: on

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
