package scala.cli.commands.default

import caseapp.*
import caseapp.core.Indexed

import scala.build.Logger
import scala.cli.ScalaCli
import scala.cli.ScalaCli.{fullRunnerName, progName}
import scala.cli.commands.bloop.BloopExit
import scala.cli.commands.default.LegacyScalaOptions.*
import scala.cli.commands.package0.Package
import scala.cli.commands.shared.ScalacOptions.YScriptRunnerOption
import scala.cli.commands.tags

/** Options covering backwards compatibility with the old scala runner.
  */
// format: off
case class LegacyScalaOptions(
  @Group("Legacy Scala runner")
  @HelpMessage(s"Ignored legacy option. Deprecated equivalent of running a subsequent `$PowerString${Package.name}` command.")
  @Tag(tags.must)
  @Hidden
  @Name("-save")
    save: Option[Indexed[Boolean]] = None,
  @Group("Legacy Scala runner")
  @HelpMessage("Ignored legacy option. Deprecated override canceling the `-nosave` option.")
  @Tag(tags.must)
  @Hidden
  @Name("-nosave")
    nosave: Option[Indexed[Boolean]] = None,
  @Group("Legacy Scala runner")
  @HelpMessage("Ignored legacy option. Deprecated override defining how the runner should treat the input. Use the appropriate sub-command instead.")
  @Tag(tags.must)
  @Hidden
  @ValueDescription("object|script|jar|repl|guess")
  @Name("-howtorun")
    howToRun: Option[Indexed[String]] = None,
  @Group("Legacy Scala runner")
  @HelpMessage("Ignored legacy option. Deprecated option allowing to preload inputs for the repl or command execution.")
  @Tag(tags.must)
  @Hidden
  @ValueDescription("file")
    I: Option[Indexed[List[String]]] = None,
  @Group("Legacy Scala runner")
  @HelpMessage("Ignored legacy option. Deprecated option allowing to prevent the use of the legacy fsc compilation daemon.")
  @Tag(tags.must)
  @Hidden
  @Name("-nc")
  @Name("-nocompdaemon")
    noCompilationDaemon: Option[Indexed[Boolean]] = None,
  @Group("Legacy Scala runner")
  @HelpMessage("Ignored legacy option. Deprecated option allowing to force the `run` mode on an input.")
  @Tag(tags.must)
  @Hidden
  @ValueDescription("file")
  @Name("-run")
    run: Option[Indexed[String]] = None,
) {
// format: on

  extension [T](indexedOption: Option[Indexed[T]]) {
    private def findArg(args: Array[String]): Option[String] =
      indexedOption.flatMap(io => args.lift(io.index))
  }

  def filterNonDeprecatedArgs(
    args: Array[String],
    progName: String,
    logger: Logger
  ): Array[String] = {
    val saveOptionString          = save.findArg(args)
    val noSaveOptionString        = nosave.findArg(args)
    val howToRunString            = howToRun.findArg(args)
    val iString                   = I.findArg(args)
    val noCompilationDaemonString = noCompilationDaemon.findArg(args)
    val runString                 = run.findArg(args)
    val deprecatedArgs =
      Seq(
        saveOptionString,
        noSaveOptionString,
        howToRunString,
        iString,
        noCompilationDaemonString,
        runString
      )
        .flatten
    val filteredArgs       = args.filterNot(deprecatedArgs.contains)
    val filteredArgsString = filteredArgs.mkString(" ")
    saveOptionString.foreach { s =>
      logger.message(
        s"""Deprecated option '$s' is ignored.
           |The compiled project files will be saved in the '.scala-build' directory in the project root folder.
           |If you need to produce an actual jar file, run the '$PowerString${Package.name}' sub-command as follows:
           |  ${Console.BOLD}$progName $PowerString${Package.name} --library $filteredArgsString${Console.RESET}""".stripMargin
      )
    }
    noSaveOptionString.foreach { ns =>
      logger.message(
        s"""Deprecated option '$ns' is ignored.
           |A jar file is not saved unless the '$PowerString${Package.name}' sub-command is called.""".stripMargin
      )
    }
    for {
      htrString <- howToRunString
      htrValue  <- howToRun.map(_.value)
    } {
      logger.message(s"Deprecated option '$htrString' is ignored.".stripMargin)
      val passedValueExplanation = htrValue match {
        case v @ ("object" | "script" | "jar") =>
          s"""$fullRunnerName does not support explicitly forcing an input to be run as '$v'.
             |Just make sure your inputs have the correct format and extension.""".stripMargin
        case "guess" =>
          s"""$fullRunnerName does not support `guess` mode.
             |Just make sure your inputs have the correct format and extension.""".stripMargin
        case "repl" =>
          s"""In order to explicitly run the repl, use the 'repl' sub-command.
             |  ${Console.BOLD}$progName repl $filteredArgsString${Console.RESET}
             |""".stripMargin
        case invalid @ _ =>
          s"""'$invalid' is not an accepted value for the '$htrString' option.
             |$fullRunnerName uses an equivalent of the old 'guess' mode by default at all times.""".stripMargin
      }
      logger.message(passedValueExplanation)
      logger.message(
        s"""Instead of the deprecated '$htrString' option, $fullRunnerName now uses a sub-command system.
           |To learn more, try viewing the help.
           |  ${Console.BOLD}$progName -help${Console.RESET}""".stripMargin
      )
    }
    for {
      optionName   <- iString
      optionValues <- I.map(_.value)
      exampleReplInputs = optionValues.mkString(" ")
    } {
      logger.message(s"Deprecated option '$optionName' is ignored.".stripMargin)
      logger.message(
        s"""To preload the extra files for the repl, try passing them as inputs for the repl sub-command.
           |  ${Console.BOLD}$progName repl $exampleReplInputs${Console.RESET}
           |""".stripMargin
      )
    }
    noCompilationDaemonString.foreach { nc =>
      logger.message(s"Deprecated option '$nc' is ignored.")
      logger.message("The script runner can no longer be picked as before.")
    }
    for {
      rString <- runString
      rValue  <- run.map(_.value)
    } {
      logger.message(s"Deprecated option '$rString' is ignored.")
      logger.message(
        s"""$fullRunnerName does not support explicitly forcing the old `run` mode.
           |Just pass $rValue as input and make sure it has the correct format and extension.
           |i.e. to run a JAR, pass it as an input, just make sure it has the `.jar` extension and a main class.
           |For details on how inputs can be run with $fullRunnerName, check the `run` sub-command.
           |  ${Console.BOLD}$progName run --help${Console.RESET}
           |""".stripMargin
      )
    }
    filteredArgs
  }
}
object LegacyScalaOptions {
  implicit lazy val parser: Parser[LegacyScalaOptions] = Parser.derive
  implicit lazy val help: Help[LegacyScalaOptions]     = Help.derive

  private[default] lazy val PowerString =
    if ScalaCli.allowRestrictedFeatures then "" else "--power "

  def yScriptRunnerWarning(yScriptRunnerValue: Option[String]): String = {
    val valueSpecificMsg = yScriptRunnerValue match {
      case Some(v @ "default") =>
        s"scala.tools.nsc.DefaultScriptRunner (the $v script runner) is no longer available."
      case Some(v @ "resident") =>
        s"scala.tools.nsc.fsc.ResidentScriptRunner (the $v script runner) is no longer available."
      case Some(v @ "shutdown") =>
        val bloopExitCommandName =
          BloopExit.names.headOption.map(_.mkString(" ")).getOrElse(BloopExit.name)
        s"""scala.tools.nsc.fsc.DaemonKiller (the $v script runner) is no longer available.
           |Did you want to stop the $fullRunnerName build server (Bloop) instead?
           |If so, consider using the following command:
           |  ${Console.BOLD}$progName $PowerString$bloopExitCommandName${Console.RESET}""".stripMargin
      case Some(className) =>
        s"Using $className as the script runner is no longer supported and will not be attempted."
      case _ => ""
    }
    s"""Deprecated option '$YScriptRunnerOption' is ignored.
       |The script runner can no longer be picked as before.
       |$valueSpecificMsg""".stripMargin
  }
}
