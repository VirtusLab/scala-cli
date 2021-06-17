package scala.cli.commands

import caseapp._

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
    repository: List[String] = Nil
)
