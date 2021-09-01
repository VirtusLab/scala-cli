package scala.cli.commands

import caseapp._

// format: off
final case class AddPathOptions(
  @Name("q")
    quiet: Boolean = false,
  title: String = ""
)
// format: on
