package scala.cli.integration

import scala.util.Properties

// format: off
class ExportSbtTests3 extends ExportSbtTestDefinitions(
  scalaVersionOpt = Some(Constants.scala3)
) {
  // format: on

  if (!Properties.isWin)
    test("repository") {
      simpleTest(ExportTestProjects.repositoryScala3Test(actualScalaVersion))
    }

  if (!Properties.isWin)
    test("main class") {
      simpleTest(
        ExportTestProjects.mainClassScala3Test(actualScalaVersion),
        extraExportArgs = Seq("--main-class", "Test")
      )
    }

  if (!Properties.isWin)
    test("test framework") {
      simpleTest(
        ExportTestProjects.testFrameworkTest(actualScalaVersion),
        sbtArgs = Seq("test")
      )
    }

}
