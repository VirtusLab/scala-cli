package scala.cli.commands

import caseapp._

// format: off
final case class SharedDependencyOptions(
  @Group("Dependency")
  @HelpMessage("Add dependencies")
  @Name("dep")
  @Name("d")
    dependency: List[String] = Nil,

  @Group("Dependency")
  @HelpMessage("Add repositories")
  @HelpMessage("")
  @Name("repo")
  @Name("r")
    repository: List[String] = Nil,
  @Group("Scala")
  @Name("P")
  @Name("plugin")
  @HelpMessage("Add compiler plugin dependencies")
  compilerPlugin: List[String] = Nil
)
// format: on

object SharedDependencyOptions {
  lazy val parser: Parser[SharedDependencyOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedDependencyOptions, parser.D] = parser
  implicit lazy val help: Help[SharedDependencyOptions]                      = Help.derive
}
