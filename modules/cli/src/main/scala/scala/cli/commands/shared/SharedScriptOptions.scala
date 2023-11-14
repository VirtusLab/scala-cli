package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

final case class SharedScriptOptions(
  @HelpMessage("Force object wrapper for scripts")
  @Tag(tags.experimental)
  objectWrapper: Option[Boolean] = None,
  @HelpMessage("Make script wrapper extend DelayedInit (this trait is deprecated since 2.11.0)")
  @Tag(tags.experimental)
  delayedInit: Option[Boolean] = None
)

object SharedScriptOptions {
  implicit lazy val parser: Parser[SharedScriptOptions] = Parser.derive
  implicit lazy val help: Help[SharedScriptOptions]     = Help.derive
}
