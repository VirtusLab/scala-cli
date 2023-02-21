package scala.cli.commands.test

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.shared.{
  CrossOptions,
  HasSharedOptions,
  HelpMessages,
  SharedJavaOptions,
  SharedOptions,
  SharedWatchOptions
}
import scala.cli.commands.tags

@HelpMessage(TestOptions.helpMessage, "", TestOptions.detailedHelpMessage)
// format: off
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
  @Tag(tags.inShortHelp)
    testFramework: Option[String] = None,

  @Group("Test")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @HelpMessage("Fail if no test suites were run")
    requireTests: Boolean = false,
  @Group("Test")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @HelpMessage("Specify a glob pattern to filter the tests suite to be run.")
    testOnly: Option[String] = None

) extends HasSharedOptions
// format: on

object TestOptions {
  implicit lazy val parser: Parser[TestOptions] = Parser.derive
  implicit lazy val help: Help[TestOptions]     = Help.derive

  val cmdName             = "test"
  private val helpHeader  = "Compile and test Scala code."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |Test sources are compiled separately (after the 'main' sources), and may use different dependencies, compiler options, and other configurations.
       |A source file is treated as a test source if:
       |  - it contains the `//> using target.scope "test"` directive
       |  - the file name ends with `.test.scala`
       |  - the file comes from a directory that is provided as input, and the relative path from that file to its original directory contains a `test` directory
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
