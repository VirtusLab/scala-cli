package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

// format: off
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
    testFramework: Option[String] = None,

  @Group("Test")
  @HelpMessage("Fail if no test suites were run")
    requireTests: Boolean = false
) {
  // format: on
  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts = baseOptions.javaOptions.javaOpts ++ sharedJava.allJavaOpts
      ),
      testOptions = baseOptions.testOptions.copy(
        frameworkOpt = testFramework.map(_.trim).filter(_.nonEmpty)
      ),
      internalDependencies = baseOptions.internalDependencies.copy(
        addTestRunnerDependencyOpt = Some(true)
      )
    )
  }
}

object TestOptions {
  implicit val parser = Parser[TestOptions]
  implicit val help   = Help[TestOptions]
}
