package scala.cli.commands

import caseapp._

// format: off
final case class SharedWatchOptions(

  @HelpMessage("Watch sources for changes")
  @Name("w")
    watch: Boolean = false

)
// format: on
