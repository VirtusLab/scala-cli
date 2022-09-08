package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

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
  @Name("printClasspath")
  @HelpMessage("Print the resulting class path")
    printClassPath: Boolean = false,

  @Name("output-directory")
  @HelpMessage("Copy compilation results to output directory using either relative or absolute path")
  @ValueDescription("/example/path")
    output: Option[String] = None,

  @HelpMessage("Compile test scope")
    test: Boolean = false
)
  // format: on

object CompileOptions {
  implicit lazy val parser: Parser[CompileOptions] = Parser.derive
  implicit lazy val help: Help[CompileOptions]     = Help.derive
}
