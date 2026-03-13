package scala.build.tests

class BuildTestsScalac extends BuildTests(server = false) {

  test("warn about Java files in mixed compilation with --server=false") {
    val recordingLogger = new RecordingLogger()
    val inputs          = TestInputs(
      os.rel / "Side.java" ->
        """public class Side {
          |    public static String message = "Hello";
          |}
          |""".stripMargin,
      os.rel / "Main.scala" ->
        """@main def main() = println(Side.message)
          |""".stripMargin
    )
    val options = defaultScala3Options.copy(useBuildServer = Some(false))
    inputs.withBuild(options, buildThreads, bloopConfigOpt, logger = Some(recordingLogger)) {
      (_, _, maybeBuild) =>
        assert(maybeBuild.isRight)
        val hasWarning = recordingLogger.messages.exists { msg =>
          msg.contains(".java files are not compiled to .class files") &&
          msg.contains("--server=false") &&
          msg.contains("Affected .java files")
        }
        assert(
          hasWarning,
          s"Expected warning about Java files with --server=false in: ${recordingLogger.messages.mkString("\n")}"
        )
    }
  }
}
