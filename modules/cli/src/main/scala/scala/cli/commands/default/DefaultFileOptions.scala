package scala.cli.commands.default

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup, HelpMessages}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Generates default files for a $fullRunnerName project (i.e. .gitignore).
     |
     |${HelpMessages.commandDocWebsiteReference("misc/default-file")}""".stripMargin)
final case class DefaultFileOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Group(HelpGroup.Default.toString)
  @HelpMessage("Write result to files rather than to stdout")
  @Tag(tags.restricted)
    write: Boolean = false,
  @Group(HelpGroup.Default.toString)
  @HelpMessage("List available default files")
  @Tag(tags.restricted)
    list: Boolean = false,
  @Group(HelpGroup.Default.toString)
  @HelpMessage("List available default file ids")
  @Tag(tags.restricted)
    listIds: Boolean = false,
  @Group(HelpGroup.Default.toString)
  @HelpMessage("Force overwriting destination files")
  @ExtraName("f")
  @Tag(tags.restricted)
    force: Boolean = false
) extends HasGlobalOptions
// format: on

object DefaultFileOptions {
  implicit lazy val parser: Parser[DefaultFileOptions] = Parser.derive
  implicit lazy val help: Help[DefaultFileOptions]     = Help.derive
}
