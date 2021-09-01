package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.{Arg, Error}
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter

abstract class ScalaCommand[T](implicit parser: Parser[T], help: Help[T])
    extends Command()(parser, help) with NeedsArgvCommand {
  def sharedOptions(t: T): Option[SharedOptions] = None
  override def hasFullHelp                       = true

  protected var argvOpt = Option.empty[Array[String]]
  override def setArgv(argv: Array[String]): Unit = {
    argvOpt = Some(argv)
  }

  // TODO Manage to have case-app give use the exact command name that was used instead
  protected def commandLength = 1

  override def error(message: Error): Nothing = {
    System.err.println(message.message)

    for (argv <- argvOpt if argv.length >= 1 + commandLength) {
      System.err.println()
      System.err.println("To list all available options, run")
      System.err.println(
        s"  ${Console.BOLD}${argv.take(1 + commandLength).mkString(" ")} --help${Console.RESET}"
      )
    }

    sys.exit(1)
  }

  // FIXME Report this in case-app default NameFormatter
  override lazy val nameFormatter: Formatter[Name] = {
    val parent = super.nameFormatter
    new Formatter[Name] {
      def format(t: Name): String =
        if (t.name.startsWith("-")) t.name
        else parent.format(t)
    }
  }

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
              val (fromIndex, completions) = cache.logger.use {
                coursier.complete.Complete(cache)
                  .withInput(prefix)
                  .complete()
                  .unsafeRun()(cache.ec)
              }
              if (completions.isEmpty) Nil
              else {
                val prefix0 = prefix.take(fromIndex)
                val values  = completions.map(c => prefix0 + c)
                values.map { str =>
                  CompletionItem(str)
                }
              }
            }
          case "repository" => Nil // TODO
          case _            => Nil
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
