package scala.cli.integration

import scala.util.Properties

// format: off
class ExportSbtTests213 extends ExportSbtTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
  // format: on

  if (!Properties.isWin)
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion))
    }

  if (!Properties.isWin)
    test("pure java") {
      simpleTest(
        ExportTestProjects.pureJavaTest,
        extraExportArgs = Seq("--sbt-setting=fork := true")
      )
    }

  if (!Properties.isWin)
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion))
    }

}
