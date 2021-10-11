package scala.cli.integration

// format: off
class ExportSbtTests213 extends ExportSbtTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
) {
  // format: on

  if (runExportTests)
    test("scalac options") {
      simpleTest(ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion))
    }

  if (runExportTests)
    test("pure java") {
      simpleTest(
        ExportTestProjects.pureJavaTest,
        extraExportArgs = Seq("--sbt-setting=fork := true")
      )
    }

  if (runExportTests)
    test("custom JAR") {
      simpleTest(ExportTestProjects.customJarTest(actualScalaVersion))
    }

}
