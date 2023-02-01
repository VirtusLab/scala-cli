package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SharedWatchOptions(

  @HelpMessage("Run the application in the background, automatically wake the thread and re-run if sources have been changed")
  @Tag(tags.should)
  @Name("w")
    watch: Boolean = false,
  @HelpMessage("Run the application in the background, automatically kill the process and restart if sources have been changed")
  @Tag(tags.should)
  @Name("revolver")
    restart: Boolean = false
) { // format: on

  lazy val watchMode: Boolean = watch || restart
}

object SharedWatchOptions {
  implicit lazy val parser: Parser[SharedWatchOptions] = Parser.derive
  implicit lazy val help: Help[SharedWatchOptions]     = Help.derive
}
