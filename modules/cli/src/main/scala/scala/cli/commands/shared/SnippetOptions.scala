package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SnippetOptions(
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Allows to execute a passed string as a Scala script")
  @Tag(tags.should)
    scriptSnippet: List[String] = List.empty,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Hidden
  @Name("executeScalaScript")
  @Name("executeSc")
  @Name("e")
  @Tag(tags.should)
    executeScript: List[String] = List.empty,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Allows to execute a passed string as Scala code")
  @Tag(tags.should)
    scalaSnippet: List[String] = List.empty,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Hidden
  @Tag(tags.implementation)
    executeScala: List[String] = List.empty,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Allows to execute a passed string as Java code")
  @Tag(tags.implementation)
    javaSnippet: List[String] = List.empty,

   @Group(HelpGroup.Java.toString)
   @Tag(tags.implementation)
  @HelpMessage("A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
    executeJava: List[String] = List.empty,

  @Group(HelpGroup.Markdown.toString)
  @HelpMessage("Allows to execute a passed string as Markdown code")
  @Name("mdSnippet")
  @Tag(tags.experimental)
    markdownSnippet: List[String] = List.empty,

  @Group(HelpGroup.Markdown.toString)
  @HelpMessage("A synonym to --markdown-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Name("executeMd")
  @Tag(tags.experimental)
  @Hidden
    executeMarkdown: List[String] = List.empty,
)
// format: on

object SnippetOptions {
  implicit lazy val parser: Parser[SnippetOptions] = Parser.derive
  implicit lazy val help: Help[SnippetOptions]     = Help.derive

}
