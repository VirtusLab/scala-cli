package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.parser.Parser
import caseapp.core.help.Help
import caseapp.core.help.HelpFormat
import caseapp.core.complete.Completer
import caseapp.core.Arg
import caseapp.core.complete.CompletionItem

abstract class ScalaCommand[T](implicit parser: Parser[T], help: Help[T]) extends Command()(parser, help) {
  def sharedOptions(t: T): Option[SharedOptions] = None
  override def completer: Completer[T] = {
    val parent = super.completer
    new Completer[T] {
      def optionName(prefix: String, state: Option[T]) =
        parent.optionName(prefix, state)
      def optionValue(arg: Arg, prefix: String, state: Option[T]) = {
        val candidates = arg.name.name match {
          case "dependency" =>
            state.flatMap(sharedOptions).toList.flatMap { sharedOptions =>
              val cache = sharedOptions.coursierCache
              sharedOptions.buildOptions(false, None, ignoreErrors = true).scalaOptions.scalaVersion
              val (fromIndex, completions) = coursier.complete.Complete(cache)
                .withInput(prefix)
                .complete()
                .unsafeRun()(cache.ec)
              if (completions.isEmpty) Nil
              else {
                val prefix0 = prefix.take(fromIndex)
                val values = completions.map(c => prefix0 + c)
                values.map { str =>
                  CompletionItem(str)
                }
              }
            }
          case "repository" => Nil // TODO
          case _ => Nil
        }
        candidates ++ parent.optionValue(arg, prefix, state)
      }
      def argument(prefix: String, state: Option[T]) =
        parent.argument(prefix, state)
    }
  }
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
