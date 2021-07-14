package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

@HelpMessage("Compile and test Scala code")
final case class TestOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),

  @Group("Test")
  @HelpMessage("Test framework to use to run tests")
  @ValueDescription("class-name")
    testFramework: Option[String] = None
) {
  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
  def testFrameworkOpt: Option[String] =
    testFramework.map(_.trim).filter(_.nonEmpty)
}

object TestOptions {
  implicit val parser = Parser[TestOptions]
  implicit val help = Help[TestOptions]
}
