package scala.cli.integration

class ReplTests213 extends ReplTestDefinitions with ReplAmmoniteTestDefinitions with Test213 {
  test("repl custom repositories work".flaky) {
    dryRun(
      cliOptions =
        Seq(
          "--scala",
          Constants.scalaSnapshot213,
          "--repository",
          "https://scala-ci.typesafe.com/artifactory/scala-integration"
        ),
      useExtraOptions = false
    )
  }
}
