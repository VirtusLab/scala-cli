package scala.cli.commands

import caseapp._

import java.nio.file.Path

// format: off
final case class BspOptions(
  // FIXME There might be too many options in SharedOptions for the bsp command…
  @Recurse
    shared: SharedOptions = SharedOptions(),
  
  @HelpMessage("Command-line options JSON file")
  @ValueDescription("path")
  @Hidden
  jsonOptions: Option[Path] = None
) {
  // format: on
}

object BspOptions {
  implicit lazy val parser: Parser[BspOptions] = Parser.derive
  implicit lazy val help: Help[BspOptions]     = Help.derive
}
