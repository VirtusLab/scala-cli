package scala.cli.commands

import caseapp._

final case class SharedWatchOptions(

  @HelpMessage("Watch sources for changes")
  @Name("w")
    watch: Boolean = false

)
