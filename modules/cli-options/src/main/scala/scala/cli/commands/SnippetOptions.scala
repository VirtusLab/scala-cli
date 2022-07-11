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
    scriptSnippet: Option[String] = None,

  @Group("Scala")
  @HelpMessage("Allows to execute a passed string as Scala code")
  @Name("executeScala")
    scalaSnippet: Option[String] = None,

  @Group("Java")
  @HelpMessage("Allows to execute a passed string as Java code")
  @Name("executeJava")
    javaSnippet: Option[String] = None,
)
// format: on

object SnippetOptions {
  implicit lazy val parser: Parser[SnippetOptions] = Parser.derive
  implicit lazy val help: Help[SnippetOptions]     = Help.derive
  // Parser.Aux for using ExpressionOptions with @Recurse in other options
  implicit lazy val parserAux: Parser.Aux[SnippetOptions, parser.D] = parser

}
