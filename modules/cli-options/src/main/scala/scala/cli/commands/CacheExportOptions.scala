package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Export Coursier Cache")
final case class CacheExportOptions(
  @Recurse
    runOptions: RunOptions = RunOptions(),
  @HelpMessage("Set the destination path")
  @Name("o")
    output: Option[String] = None,
)
  // format: on

object CacheExportOptions {
  implicit lazy val parser: Parser[CacheExportOptions] = Parser.derive
  implicit lazy val help: Help[CacheExportOptions]     = Help.derive
}
