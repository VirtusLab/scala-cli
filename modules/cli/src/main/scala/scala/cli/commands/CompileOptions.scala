package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.build.Os
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

  @Name("output-directory")
  @HelpMessage("Copy compilation results to output directory using either relative or absolute path")
  @ValueDescription("/example/path")
    output: Option[String] = None,

  @HelpMessage("Compile test scope")
    test: Boolean = false
) {
  // format: on

  def buildOptions: BuildOptions =
    shared.buildOptions(enableJmh = false, jmhVersion = None)

  def outputPath = output.filter(_.nonEmpty).map(p => os.Path(p, Os.pwd))

}

object CompileOptions {
  implicit lazy val parser: Parser[CompileOptions] = Parser.derive
  implicit lazy val help: Help[CompileOptions]     = Help.derive
}
