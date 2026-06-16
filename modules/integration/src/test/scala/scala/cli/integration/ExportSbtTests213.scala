package scala.cli.integration

trait ExportSbtTests213 { this: ExportSbtTestDefinitions & TestScalaVersion & TestSbtVersion =>
  if runExportTests then
    test(s"scalac options$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        simpleTest(
          inputs = ExportTestProjects.scalacOptionsScala2Test(actualScalaVersion),
          mainClass = None,
          extraExportArgs = defaultExportCommandArgs
        )
      }
    }
    test(s"pure java$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        simpleTest(
          inputs = ExportTestProjects.pureJavaTest("ScalaCliJavaTest"),
          mainClass = None,
          extraExportArgs = defaultExportCommandArgs ++ Seq("--sbt-setting=fork := true")
        )
      }
    }
    test(s"custom JAR$commonTestDescriptionSuffix") {
      TestUtil.retryOnCi() {
        simpleTest(
          inputs = ExportTestProjects.customJarTest(actualScalaVersion),
          mainClass = None,
          extraExportArgs = defaultExportCommandArgs
        )
      }
    }
}
