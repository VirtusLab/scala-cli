package scala.cli.integration

import scala.util.Properties

// format: off
class ExportMillTests3 extends ExportMillTestDefinitions(
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
        millArgs = Seq("__.test")
      )
    }

}
