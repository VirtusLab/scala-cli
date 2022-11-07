package scala.cli.commands

import caseapp.*

// format: off
final case class SharedWatchOptions(

  @HelpMessage("Watch source files for changes")
  @Name("w")
    watch: Boolean = false,
  @HelpMessage("Run your application in background and automatically restart if sources have been changed")
  @Name("revolver")
    restart: Boolean = false
) { // format: on

  lazy val watchMode = watch || restart
}

object SharedWatchOptions {
  implicit lazy val parser: Parser[SharedWatchOptions] = Parser.derive
  implicit lazy val help: Help[SharedWatchOptions]     = Help.derive
}
