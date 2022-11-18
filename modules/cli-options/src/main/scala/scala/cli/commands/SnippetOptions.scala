package scala.cli.commands

import caseapp.*

// format: off
final case class SnippetOptions(
  @Group("Scala")
  @HelpMessage("Allows to execute a passed string as a Scala script")
  @Tag(tags.should)
    scriptSnippet: List[String] = List.empty,

  @Group("Scala")
  @HelpMessage("A synonym to --script-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Hidden
  @Name("executeScalaScript")
  @Name("executeSc")
  @Name("e")
  @Tag(tags.should)
    executeScript: List[String] = List.empty,

  @Group("Scala")
  @HelpMessage("Allows to execute a passed string as Scala code")
  @Tag(tags.should)
    scalaSnippet: List[String] = List.empty,

  @Group("Scala")
  @HelpMessage("A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Hidden
  @Tag(tags.implementation)
    executeScala: List[String] = List.empty,

  @Group("Java")
  @HelpMessage("Allows to execute a passed string as Java code")
  @Tag(tags.implementation)
    javaSnippet: List[String] = List.empty,

   @Group("Java")
   @Tag(tags.implementation)
  @HelpMessage("A synonym to --scala-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
    executeJava: List[String] = List.empty,

  @Group("Markdown")
  @HelpMessage("[experimental] Allows to execute a passed string as Markdown code")
  @Name("mdSnippet")
    markdownSnippet: List[String] = List.empty,

  @Group("Markdown")
  @HelpMessage("[experimental] A synonym to --markdown-snippet, which defaults the sub-command to `run` when no sub-command is passed explicitly")
  @Name("executeMd")
  @Hidden
    executeMarkdown: List[String] = List.empty,
)
// format: on

object SnippetOptions {
  implicit lazy val parser: Parser[SnippetOptions] = Parser.derive
  implicit lazy val help: Help[SnippetOptions]     = Help.derive

}
