package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.Build
import scala.build.options.BuildOptions

// format: off
@HelpMessage("Compile and run Scala code")
final case class RunOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    benchmarking: BenchmarkingOptions = BenchmarkingOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),

  @Group("Runner")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None
) {
  // format: on

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(
      enableJmh = benchmarking.jmh.contains(true),
      jmhVersion = benchmarking.jmhVersion
    )
    baseOptions.copy(
      mainClass = mainClass,
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts = baseOptions.javaOptions.javaOpts ++ sharedJava.allJavaOpts
      )
    )
  }
}

object RunOptions {
  implicit val parser = Parser[RunOptions]
  implicit val help   = Help[RunOptions]
}
