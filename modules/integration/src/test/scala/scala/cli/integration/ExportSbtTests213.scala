package scala.cli.integration

class ExportSbtTests213 extends ExportSbtTestDefinitions with Test213 {
  if (runExportTests) {
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion), mainClass = None)
    }
    test("pure java") {
      simpleTest(
        ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
        mainClass = None,
        extraExportArgs = Seq("--sbt-setting=fork := true")
      )
    }
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion), mainClass = None)
    }
  }
}
