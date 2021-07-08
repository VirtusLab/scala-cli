package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.Build

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
)

object RunOptions {
  implicit val parser = Parser[RunOptions]
  implicit val help = Help[RunOptions]
}
