package scala.cli.commands.compile

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.shared.{CrossOptions, HasSharedOptions, SharedOptions, SharedWatchOptions}
import scala.cli.commands.tags

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
  @Tag(tags.restricted)
  @HelpMessage("Print the resulting class path")
    printClassPath: Boolean = false,

  @HelpMessage("Compile test scope")
  @Tag(tags.should)
    test: Boolean = false
) extends HasSharedOptions
  // format: on

object CompileOptions {
  implicit lazy val parser: Parser[CompileOptions] = Parser.derive
  implicit lazy val help: Help[CompileOptions]     = Help.derive
}
