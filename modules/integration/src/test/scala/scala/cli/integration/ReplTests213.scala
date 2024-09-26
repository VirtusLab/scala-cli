package scala.cli.integration

class ReplTests213 extends ReplTestDefinitions with ReplAmmoniteTestDefinitions with Test213 {
  test("repl custom repositories work".flaky) {
    TestInputs.empty.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "repl",
        "--repl-dry-run",
        "--scala",
        Constants.scalaSnapshot213,
        "--repository",
        "https://scala-ci.typesafe.com/artifactory/scala-integration"
      ).call(cwd = root)
    }
  }
}
