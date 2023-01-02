package scala.cli.commands.default

import caseapp.core.help.{Help, HelpCompanion, RuntimeCommandsHelp}
import caseapp.core.{Error, RemainingArgs}

import scala.build.Logger
import scala.build.internal.Constants
import scala.build.options.BuildOptions
import scala.cli.CurrentParams
import scala.cli.commands.repl.{Repl, ReplOptions}
import scala.cli.commands.shared.ScalaCliHelp.helpFormat
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.shebang.{Shebang, ShebangOptions}
import scala.cli.commands.version.Version
import scala.cli.commands.{ScalaCommand, ScalaCommandWithCustomHelp}
import scala.cli.launcher.LauncherOptions

class Default(
  actualHelp: => RuntimeCommandsHelp,
  isSipScala: Boolean
) extends ScalaCommandWithCustomHelp[DefaultOptions](actualHelp) {

  private lazy val defaultCommandHelp: String =
    s"""
       |
       |When no subcommand is passed explicitly, an implicit subcommand is used based on context:
       |  - if the '--version' option is passed, it prints the 'version' subcommand output, unmodified by any other options
       |  - if any inputs were passed, it defaults to the 'shebang' subcommand
       |  - additionally, when no inputs were passed, it defaults to the 'shebang' subcommand in the following scenarios:
       |    - if a snippet was passed with any of the '--execute*' options
       |    - if a main class was passed with the '--main-class' option alongside an extra '--classpath'
       |  - otherwise, if no inputs were passed, it defaults to the 'repl' subcommand""".stripMargin

  override def customHelp(showHidden: Boolean): String =
    super.customHelp(showHidden) + defaultCommandHelp

  override def scalaSpecificationLevel = SpecificationLevel.MUST

  override def group = "Main"

  override def sharedOptions(options: DefaultOptions): Option[SharedOptions] = Some(options.shared)

  private[cli] var rawArgs = Array.empty[String]

  override def runCommand(options: DefaultOptions, args: RemainingArgs, logger: Logger): Unit =
    if options.version then println(Version.versionInfo)
    else
      {
        val shouldDefaultToShebang =
          args.remaining.nonEmpty || options.shared.snippet.executeScript.nonEmpty ||
          options.shared.snippet.executeScala.nonEmpty || options.shared.snippet.executeJava.nonEmpty ||
          options.shared.snippet.executeMarkdown.nonEmpty ||
          (options.shared.extraJarsAndClassPath.nonEmpty && options.sharedRun.mainClass.mainClass.nonEmpty)
        if shouldDefaultToShebang then ShebangOptions.parser else ReplOptions.parser
      }.parse(options.legacyScala.filterNonDeprecatedArgs(rawArgs, progName, logger)) match
        case Left(e)                               => error(e)
        case Right((replOptions: ReplOptions, _))  => Repl.runCommand(replOptions, args, logger)
        case Right((sbOptions: ShebangOptions, _)) => Shebang.runCommand(sbOptions, args, logger)
}
