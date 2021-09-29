package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.options.BuildOptions

// format: off
@HelpMessage("Compile Scala code")
final case class CompileOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    watch: SharedWatchOptions = SharedWatchOptions(),
  @Recurse
    cross: CrossOptions = CrossOptions(),

  @Name("p")
  @Name("classpath")
  @HelpMessage("Print resulting class path")
    classPath: Boolean = false
) {
  // format: on

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)
}

object CompileOptions {
  implicit val parser = Parser[CompileOptions]
  implicit val help   = Help[CompileOptions]
}
