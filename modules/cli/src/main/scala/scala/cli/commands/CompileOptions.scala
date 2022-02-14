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
  @HelpMessage("Print the resulting class path")
    classPath: Boolean = false,
  @HelpMessage("Compile test scope")
    test: Boolean = false
) {
  // format: on

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None, shouldCompileTest = Some(test))

}

object CompileOptions {
  implicit lazy val parser: Parser[CompileOptions] = Parser.derive
  implicit lazy val help: Help[CompileOptions]     = Help.derive
}
