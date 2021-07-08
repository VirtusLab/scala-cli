package scala.cli.commands

import caseapp._

final case class AddPathOptions(
  @Name("q")
    quiet: Boolean = false,
  title: String = ""
)
