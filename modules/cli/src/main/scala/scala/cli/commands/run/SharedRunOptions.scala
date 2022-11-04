package scala.cli.commands.run

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.shared.{
  BenchmarkingOptions,
  CrossOptions,
  MainClassOptions,
  SharedJavaOptions,
  SharedPythonOptions,
  SharedWatchOptions
}
import scala.cli.commands.tags

// format: off
final case class SharedRunOptions(
  @Recurse
    benchmarking: BenchmarkingOptions = BenchmarkingOptions(),
  @Recurse
    sharedJava: SharedJavaOptions = SharedJavaOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    compileCross: CrossOptions = CrossOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),
  @Group("Run")
  @Hidden
  @Tag(tags.experimental)
  @HelpMessage("Run as a Spark job, using the spark-submit command")
  @ExtraName("spark")
    sparkSubmit: Option[Boolean] = None,
  @Group("Run")
  @Hidden
  @Tag(tags.experimental)
  @HelpMessage("Spark-submit arguments")
  @ExtraName("submitArg")
    submitArgument: List[String] = Nil,
  @Group("Run")
  @Tag(tags.experimental)
  @HelpMessage("Run as a Spark job, using a vanilla Spark distribution downloaded by Scala CLI")
  @ExtraName("sparkStandalone")
    standaloneSpark: Option[Boolean] = None,
  @Group("Run")
  @Tag(tags.experimental)
  @HelpMessage("Run as a Hadoop job, using the \"hadoop jar\" command")
  @ExtraName("hadoop")
    hadoopJar: Boolean = false,
  @Group("Run")
  @Tag(tags.should)
  @HelpMessage("Print the command that would have been run (one argument per line), rather than running it")
    command: Boolean = false,
  @Group("Run")
  @HelpMessage("Temporary / working directory where to write generated launchers")
    scratchDir: Option[String] = None,
  @Group("Run")
  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Run Java commands using a manifest-based class path (shortens command length)")
    useManifest: Option[Boolean] = None
)
// format: on

object SharedRunOptions {
  implicit lazy val parser: Parser[SharedRunOptions] = Parser.derive
  implicit lazy val help: Help[SharedRunOptions]     = Help.derive
}
