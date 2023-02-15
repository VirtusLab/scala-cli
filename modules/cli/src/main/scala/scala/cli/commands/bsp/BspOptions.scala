package scala.cli.commands.bsp

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Start BSP server.
     |
     |BSP stands for Build Server Protocol.
     |For more information refer to https://build-server-protocol.github.io/
     |
     |This sub-command is not designed to be used by a human.
     |It is normally supposed to be invoked by your IDE when a $fullRunnerName project is imported.
     |
     |${HelpMessages.docsWebsiteReference}""".stripMargin)
final case class BspOptions(
  // FIXME There might be too many options in SharedOptions for the bsp command…
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @HelpMessage("Command-line options JSON file")
  @ValueDescription("path")
  @Hidden
  @Tag(tags.implementation)
    jsonOptions: Option[String] = None
) extends HasSharedOptions {
  // format: on
}

object BspOptions {
  implicit lazy val parser: Parser[BspOptions] = Parser.derive
  implicit lazy val help: Help[BspOptions]     = Help.derive
}
