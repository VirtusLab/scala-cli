package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("""|Compile and run Scala code.
                |
                |To pass arguments to the application, just add them after `--`, like:
                |
                |```sh
                |scala-cli MyApp.scala -- first-arg second-arg
                |```""".stripMargin)
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
    mainClass: MainClassOptions = MainClassOptions(),
  @Group("Run")
  @Hidden
  @HelpMessage("Run as a Spark job, using the spark-submit command")
  @ExtraName("spark")
    sparkSubmit: Option[Boolean] = None,
  @Group("Run")
  @HelpMessage("Print the command that would have been run (one argument per line), rather than running it")
    command: Boolean = false,
  @Group("Run")
  @HelpMessage("Temporary / working directory where to write generated launchers")
    scratchDir: Option[String] = None,
  @Group("Run")
  @Hidden
  @HelpMessage("Run Java commands using a manifest-based class path (shortens command length)")
    useManifest: Option[Boolean] = None
)
// format: on

object RunOptions {
  implicit lazy val parser: Parser[RunOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]     = Help.derive
}
