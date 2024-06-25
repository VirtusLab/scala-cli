package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

case class ScopeOptions(
  @Group(HelpGroup.Compilation.toString)
  @HelpMessage("Include test scope")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
  @Name("testScope")
  @Name("withTestScope")
  @Name("withTest")
  test: Boolean = false
)
object ScopeOptions {
  implicit lazy val parser: Parser[ScopeOptions] = Parser.derive
  implicit lazy val help: Help[ScopeOptions]     = Help.derive
}
