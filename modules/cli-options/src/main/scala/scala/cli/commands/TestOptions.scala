package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Compile and test Scala code")
final case class TestOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),

  @Group("Test")
  @HelpMessage("Name of the test framework's runner class to use while running tests")
  @ValueDescription("class-name")
    testFramework: Option[String] = None,

  @Group("Test")
  @HelpMessage("Fail if no test suites were run")
    requireTests: Boolean = false
)
// format: on

object TestOptions {
  implicit lazy val parser: Parser[TestOptions] = Parser.derive
  implicit lazy val help: Help[TestOptions]     = Help.derive
}
