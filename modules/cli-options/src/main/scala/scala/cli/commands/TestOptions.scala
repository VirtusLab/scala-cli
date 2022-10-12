package scala.cli.commands

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.common.HasSharedOptions

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
    compileCross: CrossOptions = CrossOptions(),

  @Group("Test")
  @HelpMessage("Name of the test framework's runner class to use while running tests")
  @ValueDescription("class-name")
  @Tag(tags.should)
    testFramework: Option[String] = None,

  @Group("Test")
  @Tag(tags.should)
  @HelpMessage("Fail if no test suites were run")
    requireTests: Boolean = false
) extends HasSharedOptions
// format: on

object TestOptions {
  implicit lazy val parser: Parser[TestOptions] = Parser.derive
  implicit lazy val help: Help[TestOptions]     = Help.derive
}
