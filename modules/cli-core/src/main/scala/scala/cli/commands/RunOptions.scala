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

  @Group("Runner")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None
) {
  def retainedMainClass: Option[String] =
    // TODO Warn if users passed a main class along with --jmh
    if (benchmarking.enableJmh) Some("org.openjdk.jmh.Main")
    else mainClass

  def buildOptions(scalaVersions: ScalaVersions): Build.Options =
    shared.buildOptions(scalaVersions, benchmarking.enableJmh, benchmarking.jmhVersion)
}

object RunOptions {
  implicit val parser = Parser[RunOptions]
  implicit val help = Help[RunOptions]
}
