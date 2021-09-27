package scala.cli.integration

// format: off
class ExportMillTests3 extends ExportMillTestDefinitions(
  scalaVersionOpt = Some(Constants.scala3)
) {
  // format: on

  test("repository") {
    simpleTest(ExportTestProjects.repositoryScala3Test(actualScalaVersion))
  }

  test("main class") {
    simpleTest(
      ExportTestProjects.mainClassScala3Test(actualScalaVersion),
      extraExportArgs = Seq("--main-class", "Test")
    )
  }

  test("test framework") {
    simpleTest(
      ExportTestProjects.testFrameworkTest(actualScalaVersion),
      millArgs = Seq("__.test")
    )
  }

}
