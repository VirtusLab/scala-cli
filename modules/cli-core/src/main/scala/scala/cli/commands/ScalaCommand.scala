package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.help.Help
import caseapp.core.help.HelpFormat

abstract class ScalaCommand[T](implicit parser: Parser[T], help: Help[T]) extends Command()(parser, help) {
  override def helpFormat =
    HelpFormat.default()
      .withSortedGroups(Some(Seq(
        "Help",
        "Scala",
        "Java",
        "Repl",
        "Package",
        "Metabrowse server",
        "Logging",
        "Runner"
      )))
      .withSortedCommandGroups(Some(Seq(
        "Main",
        "Miscellaneous",
        ""
      )))
}
