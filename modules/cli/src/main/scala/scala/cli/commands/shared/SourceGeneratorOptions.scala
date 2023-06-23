package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SourceGeneratorOptions(
  @Group(HelpGroup.SourceGenerator.toString)
  @Tag(tags.experimental)
  @HelpMessage("Generate BuildInfo for project")
  @Name("buildInfo")
    useBuildInfo: Option[Boolean] = None,
)
// format: on

object SourceGeneratorOptions {
  implicit lazy val parser: Parser[SourceGeneratorOptions] = Parser.derive
  implicit lazy val help: Help[SourceGeneratorOptions]     = Help.derive
}
