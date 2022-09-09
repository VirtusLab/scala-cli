package scala.cli.commands

import caseapp.Name
import caseapp.core.app.Command
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser
import caseapp.core.util.Formatter
import caseapp.core.{Arg, Error}

import scala.annotation.tailrec
import scala.build.compiler.SimpleScalaCompiler
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope}
import scala.cli.ScalaCli
import scala.cli.commands.util.CommandHelpers
import scala.cli.commands.util.SharedOptionsUtil.*
import scala.util.{Properties, Try}

abstract class ScalaCommand[T](implicit parser: Parser[T], help: Help[T])
    extends Command()(parser, help) with NeedsArgvCommand with CommandHelpers {
  def sharedOptions(t: T): Option[SharedOptions] = // hello borked unused warning
    None
  override def hasFullHelp = true

  def isRestricted: Boolean = false

  protected var argvOpt = Option.empty[Array[String]]
  override def setArgv(argv: Array[String]): Unit = {
    argvOpt = Some(argv)
  }

  /** @return the actual Scala CLI program name which was run */
  protected def progName: String = ScalaCli.progName

  // TODO Manage to have case-app give use the exact command name that was used instead
  /** The actual sub-command name that was used. If the sub-command name is a list of strings, space
    * is used as the separator. If [[argvOpt]] hasn't been defined, it defaults to [[name]].
    */
  protected def actualCommandName: String =
    argvOpt.map { argv =>
      @tailrec
      def validCommand(potentialCommandName: List[String]): Option[List[String]] =
        if potentialCommandName.isEmpty then None
        else
          names.find(_ == potentialCommandName) match {
            case cmd @ Some(_) => cmd
            case _             => validCommand(potentialCommandName.dropRight(1))
          }

      val maxCommandLength: Int    = names.map(_.length).max max 1
      val maxPotentialCommandNames = argv.slice(1, maxCommandLength + 1).toList
      validCommand(maxPotentialCommandNames).getOrElse(List(""))
    }.getOrElse(List(name)).mkString(" ")

  protected def actualFullCommand: String =
    if actualCommandName.nonEmpty then s"$progName $actualCommandName" else progName

  override def error(message: Error): Nothing = {
    System.err.println(
      s"""${message.message}
         |
         |To list all available options, run
         |  ${Console.BOLD}$actualFullCommand --help${Console.RESET}""".stripMargin
    )
    sys.exit(1)
  }

  // FIXME Report this in case-app default NameFormatter
  override lazy val nameFormatter: Formatter[Name] = {
    val parent = super.nameFormatter
    (t: Name) =>
      if (t.name.startsWith("-")) t.name
      else parent.format(t)
  }

  override def completer: Completer[T] = {
    val parent = super.completer
    new Completer[T] {
      def optionName(prefix: String, state: Option[T]): List[CompletionItem] =
        parent.optionName(prefix, state)
      def optionValue(arg: Arg, prefix: String, state: Option[T]): List[CompletionItem] = {
        val candidates = arg.name.name match {
          case "dependency" =>
            state.flatMap(sharedOptions).toList.flatMap { sharedOptions =>
              val cache = sharedOptions.coursierCache
              val sv = sharedOptions.buildOptions()
                .scalaParams
                .toOption
                .flatten
                .map(_.scalaVersion)
                .getOrElse(Constants.defaultScalaVersion)
              val (fromIndex, completions) = cache.logger.use {
                coursier.complete.Complete(cache)
                  .withInput(prefix)
                  .withScalaVersion(sv)
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
      def argument(prefix: String, state: Option[T]): List[CompletionItem] =
        parent.argument(prefix, state)
    }
  }

  def maybePrintGroupHelp(options: T): Unit =
    for (shared <- sharedOptions(options))
      shared.helpGroups.maybePrintGroupHelp(help, helpFormat)

  /** Print `scalac` output if passed options imply no inputs are necessary and raw `scalac` output
    * is required instead. (i.e. `--scalac-option -help`)
    * @param options
    *   command options
    */
  def maybePrintSimpleScalacOutput(options: T, buildOptions: BuildOptions): Unit =
    for {
      shared <- sharedOptions(options)
      scalacOptions = shared.scalac.scalacOption.toSeq
      updatedScalacOptions =
        if (shared.scalacHelp && !scalacOptions.contains("-help"))
          scalacOptions.appended("-help")
        else scalacOptions
      if updatedScalacOptions.exists(ScalacOptions.ScalacPrintOptions)
      logger = shared.logger
      artifacts      <- buildOptions.artifacts(logger, Scope.Main).toOption
      scalaArtifacts <- artifacts.scalaOpt
      compilerClassPath   = scalaArtifacts.compilerClassPath
      scalaVersion        = scalaArtifacts.params.scalaVersion
      compileClassPath    = artifacts.compileClassPath
      simpleScalaCompiler = SimpleScalaCompiler("java", Nil, scaladoc = false)
      javacOptions        = buildOptions.javaOptions.javacOptions
      javaHome            = buildOptions.javaHomeLocation().value
    } {
      val exitCode = simpleScalaCompiler.runSimpleScalacLike(
        scalaVersion,
        Option(javaHome),
        javacOptions,
        updatedScalacOptions,
        compileClassPath,
        compilerClassPath,
        logger
      )
      sys.exit(exitCode)
    }

  override def helpFormat: HelpFormat =
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
          //   val width = os.proc("sh", "-c", s"$pathedTput cols 2>/dev/tty").call(stderr = os.Pipe).out.trim().toInt
          //   Some(width)
          // Ideally, we should do an ioctl, like jansi does here:
          //   https://github.com/fusesource/jansi/blob/09722b7cccc8a99f14ac1656db3072dbeef34478/src/main/java/org/fusesource/jansi/AnsiConsole.java#L344
          // This requires writing our own minimal JNI library, that publishes '.a' files too for static linking in the executable of Scala CLI.
          None
      }
}
