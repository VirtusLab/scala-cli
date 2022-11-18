package scala.cli.commands.setupide

import caseapp.*

import scala.cli.commands.shared.{HasSharedOptions, SharedBspFileOptions, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Generate a BSP file that you can import into your IDE")
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    bspFile: SharedBspFileOptions = SharedBspFileOptions(),
  @Hidden
  @Tag(tags.implementation)
  charset: Option[String] = None
) extends HasSharedOptions
// format: on

object SetupIdeOptions {
  implicit lazy val parser: Parser[SetupIdeOptions] = Parser.derive
  implicit lazy val help: Help[SetupIdeOptions]     = Help.derive
}
