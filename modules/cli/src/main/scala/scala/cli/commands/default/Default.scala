package scala.cli.commands.default

import caseapp.core.help.RuntimeCommandsHelp
import caseapp.core.{Error, RemainingArgs}

import scala.build.Logger
import scala.build.input.{Inputs, ScalaCliInvokeData, SubCommand}
import scala.cli.commands.ScalaCommandWithCustomHelp
import scala.cli.commands.repl.{Repl, ReplOptions}
import scala.cli.commands.run.{Run, RunOptions}
import scala.cli.commands.shared.{HelpCommandGroup, SharedOptions}
import scala.cli.commands.version.{Version, VersionOptions}

class Default(actualHelp: => RuntimeCommandsHelp)
    extends ScalaCommandWithCustomHelp[DefaultOptions](actualHelp) {
  private lazy val defaultCommandHelp: String =
    s"""
       |When no subcommand is passed explicitly, an implicit subcommand is used based on context:
       |  - if the '--version' option is passed, it prints the 'version' subcommand output, unmodified by any other options
       |  - if any inputs were passed, it defaults to the 'run' subcommand
       |  - additionally, when no inputs were passed, it defaults to the 'run' subcommand in the following scenarios:
       |    - if a snippet was passed with any of the '--execute*' options
       |    - if a main class was passed with the '--main-class' option alongside an extra '--classpath'
       |  - otherwise, if no inputs were passed, it defaults to the 'repl' subcommand""".stripMargin

  override def customHelp(showHidden: Boolean): String =
    super.customHelp(showHidden) + defaultCommandHelp

  override def scalaSpecificationLevel = SpecificationLevel.MUST

  override def group: String = HelpCommandGroup.Main.toString

  override def sharedOptions(options: DefaultOptions): Option[SharedOptions] = Some(options.shared)

  private[cli] var rawArgs = Array.empty[String]

  override def invokeData: ScalaCliInvokeData =
    super.invokeData.copy(subCommand = SubCommand.Default)

  override def runCommand(options: DefaultOptions, args: RemainingArgs, logger: Logger): Unit =
    // can't fully re-parse and redirect to Version because of --cli-version and --scala-version clashing
    if options.version then Version.runCommand(VersionOptions(options.shared.global), args, logger)
    else
      {
        val shouldDefaultToRun =
          args.remaining.nonEmpty || options.shared.snippet.executeScript.nonEmpty ||
          options.shared.snippet.executeScala.nonEmpty || options.shared.snippet.executeJava.nonEmpty ||
          options.shared.snippet.executeMarkdown.nonEmpty ||
          (options.shared.extraClasspathWasPassed && options.sharedRun.mainClass.mainClass.nonEmpty)
        if shouldDefaultToRun then RunOptions.parser else ReplOptions.parser
      }.parse(options.legacyScala.filterNonDeprecatedArgs(rawArgs, progName, logger)) match
        case Left(e)                              => error(e)
        case Right((replOptions: ReplOptions, _)) => Repl.runCommand(replOptions, args, logger)
        case Right((runOptions: RunOptions, _)) =>
          Run.runCommand(
            runOptions,
            args.remaining,
            args.unparsed,
            () => Inputs.default(),
            logger,
            invokeData
          )
}
