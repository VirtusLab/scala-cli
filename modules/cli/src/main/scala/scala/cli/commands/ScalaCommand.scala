package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

import scala.build.Logger
import scala.build.errors.BuildException
import scala.cli.commands.util.SharedOptionsUtil._
import scala.util.{Properties, Try}

abstract class ScalaCommand[T](implicit parser: Parser[T], help: Help[T])
    extends Command()(parser, help) with NeedsArgvCommand {
  def sharedOptions(t: T): Option[SharedOptions] = None
  override def hasFullHelp                       = true

  def inSipScala: Boolean = true

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

  def maybePrintGroupHelp(options: T): Unit =
    for (shared <- sharedOptions(options))
      shared.helpGroups.maybePrintGroupHelp(help)

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
      .withHiddenGroups(Some(Seq(
        "Scala.js",
        "Scala Native"
      )))
      .withTerminalWidthOpt {
        if (Properties.isWin)
          if (coursier.paths.Util.useJni())
            Try(coursier.jniutils.WindowsAnsiTerminal.terminalSize()).toOption.map(
              _.getWidth
            ).orElse {
              val fallback = 120
              if (java.lang.Boolean.getBoolean("scala.cli.windows-terminal.verbose"))
                System.err.println(s"Could not get terminal width, falling back to $fallback")
              Some(fallback)
            }
          else None
        else
          // That's how Ammonite gets the terminal width, but I'd rather not spawn a sub-process upfront in Scala CLI…
          //   val pathedTput = if (os.isFile(os.Path("/usr/bin/tput"))) "/usr/bin/tput" else "tput"
          //   val width = os.proc("sh", "-c", s"$pathedTput cols 2>/dev/tty").call(stderr = os.Pipe).out.text().trim.toInt
          //   Some(width)
          // Ideally, we should do an ioctl, like jansi does here:
          //   https://github.com/fusesource/jansi/blob/09722b7cccc8a99f14ac1656db3072dbeef34478/src/main/java/org/fusesource/jansi/AnsiConsole.java#L344
          // This requires writing our own minimal JNI library, that publishes '.a' files too for static linking in the executable of Scala CLI.
          None
      }

  implicit class EitherBuildExceptionOps[E <: BuildException, T](private val either: Either[E, T]) {
    def orReport(logger: Logger): Option[T] =
      either match {
        case Left(ex) =>
          logger.log(ex)
          None
        case Right(t) => Some(t)
      }
    def orExit(logger: Logger): T =
      either match {
        case Left(ex) => logger.exit(ex)
        case Right(t) => t
      }
  }
}
