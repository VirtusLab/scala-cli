package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Import Coursier Cache")
final case class CacheImportOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Name("c")
  @Name("cache")
    cachePath: Option[String] = None,
)
  // format: on

object CacheImportOptions {
  implicit lazy val parser: Parser[CacheImportOptions] = Parser.derive
  implicit lazy val help: Help[CacheImportOptions]     = Help.derive
}
