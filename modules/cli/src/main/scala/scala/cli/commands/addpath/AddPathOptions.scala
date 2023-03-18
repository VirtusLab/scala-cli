package scala.cli.commands.addpath

import caseapp.*

import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Add entries to the PATH environment variable.")
final case class AddPathOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Tag(tags.restricted)
    title: String = ""
) extends HasGlobalOptions
// format: on

object AddPathOptions {
  implicit lazy val parser: Parser[AddPathOptions] = Parser.derive
  implicit lazy val help: Help[AddPathOptions]     = Help.derive
}
