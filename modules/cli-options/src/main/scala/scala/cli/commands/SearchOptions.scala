package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Search artifacts")
final case class SearchOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions(),

  @Group("Searching")
  @HelpMessage("Query to search for")
  @ExtraName("q")
    query: Option[String] = None,

  @Group("Searching")
  @HelpMessage("Target platform")
  @ValueDescription("JVM|JS|NATIVE|SBT")
  @ExtraName("t")
    target: Option[String] = None,

  @Group("Searching")
  @HelpMessage("Scala binary version")
  @ExtraName("sv")
    scalaBinary: Option[String] = None,

  @Group("Searching")
  @HelpMessage("Restrict to artifacts within the query")
  @ValueDescription("true|false")
    strict: Option[Boolean] = Some(true),
)
// format: on


object SearchOptions {
  implicit lazy val parser: Parser[SearchOptions] = Parser.derive
  implicit lazy val help: Help[SearchOptions]   = Help.derive
}
