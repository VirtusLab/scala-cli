package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

// format: off
@HelpMessage("""|Compile and run Scala code.
                |
                |To pass arguments to the application, just add them after '--', like:
                |scala-cli MyApp.scala -- first-arg second-arg""".stripMargin)
final case class RunOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    benchmarking: BenchmarkingOptions = BenchmarkingOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CompileCrossOptions = CompileCrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions()
) {
  // format: on

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(
      enableJmh = benchmarking.jmh.contains(true),
      jmhVersion = benchmarking.jmhVersion
    )
    baseOptions.copy(
      mainClass = mainClass.mainClass,
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
