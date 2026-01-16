package scala.cli.integration

class ExportMill1Tests213 extends ExportMillTestDefinitions with Test213 with TestMill1 {
  if runExportTests then {
    test(s"scalac options$commonTestDescriptionSuffix") {
      simpleTest(
        inputs = ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion),
        mainClass = None,
        extraExportArgs = defaultExportCommandArgs
      )
    }
    test(s"pure java$commonTestDescriptionSuffix") {
      simpleTest(
        inputs = ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
        mainClass = None,
        extraExportArgs = defaultExportCommandArgs
      )
    }
    test(s"custom JAR$commonTestDescriptionSuffix") {
      simpleTest(
        inputs = ExportTestProjects.customJarTest(actualScalaVersion),
        mainClass = None,
        extraExportArgs = defaultExportCommandArgs
      )
    }
  }
}
