package scala.cli.commands.bsp

import caseapp.*

import scala.cli.commands.shared.{HasSharedOptions, SharedOptions}

// format: off
@HelpMessage("Start BSP server")
final case class BspOptions(
  // FIXME There might be too many options in SharedOptions for the bsp command…
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @HelpMessage("Command-line options JSON file")
  @ValueDescription("path")
  @Hidden
  jsonOptions: Option[String] = None
) extends HasSharedOptions {
  // format: on
}

object BspOptions {
  implicit lazy val parser: Parser[BspOptions] = Parser.derive
  implicit lazy val help: Help[BspOptions]     = Help.derive
}
