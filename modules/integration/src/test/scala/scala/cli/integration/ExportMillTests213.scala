package scala.cli.integration

import scala.util.Properties

// format: off
class ExportMillTests213 extends ExportMillTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
  // format: on

  if (!Properties.isWin)
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion))
    }

  if (!Properties.isWin)
    test("pure java") {
      simpleTest(ExportTestProjects.pureJavaTest)
    }

  if (!Properties.isWin)
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion))
    }

}
