package scala.cli.commands

import caseapp._

// format: off
final case class SnippetOptions(
  @Group("Scala")
  @HelpMessage("Allows to execute a passed string as a Scala script")
  @Name("e")
  @Name("executeScript")
  @Name("executeScalaScript")
  @Name("executeSc")
    scriptSnippet: List[String] = List.empty,

  @Group("Scala")
  @HelpMessage("Allows to execute a passed string as Scala code")
  @Name("executeScala")
    scalaSnippet: List[String] = List.empty,

  @Group("Java")
  @HelpMessage("Allows to execute a passed string as Java code")
  @Name("executeJava")
    javaSnippet: List[String] = List.empty,
)
// format: on

object SnippetOptions {
  implicit lazy val parser: Parser[SnippetOptions] = Parser.derive
  implicit lazy val help: Help[SnippetOptions]     = Help.derive
  // Parser.Aux for using SnippetOptions with @Recurse in other options
  implicit lazy val parserAux: Parser.Aux[SnippetOptions, parser.D] = parser

}
