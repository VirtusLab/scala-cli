package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SharedBspFileOptions(
  @Group("BSP")
  @Name("bspDir")
  @HelpMessage("Custom BSP configuration location")
  @Tag(tags.implementation)
  @Hidden
    bspDirectory: Option[String] = None,
  @Group("BSP")
  @Name("name")
  @HelpMessage("Name of BSP")
  @Hidden
  @Tag(tags.implementation)
    bspName: Option[String] = None
)
// format: on

object SharedBspFileOptions {
  implicit lazy val parser: Parser[SharedBspFileOptions] = Parser.derive
  implicit lazy val help: Help[SharedBspFileOptions]     = Help.derive
}
