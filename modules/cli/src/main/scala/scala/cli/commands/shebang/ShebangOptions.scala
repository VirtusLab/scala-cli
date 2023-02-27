package scala.cli.commands.shebang

import caseapp.*

import scala.build.internal.util.ConsoleUtils.ScalaCliConsole
import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName, progName}
import scala.cli.commands.run.RunOptions
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}

@HelpMessage(ShebangOptions.helpMessage, "", ShebangOptions.detailedHelpMessage)
final case class ShebangOptions(
  @Recurse
  runOptions: RunOptions = RunOptions()
) extends HasSharedOptions {
  override def shared: SharedOptions = runOptions.shared
}

object ShebangOptions {
  implicit lazy val parser: Parser[ShebangOptions] = Parser.derive
  implicit lazy val help: Help[ShebangOptions]     = Help.derive

  val cmdName             = "shebang"
  private val helpHeader  = "Like `run`, but handier for shebang scripts."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |This command is equivalent to the `run` sub-command, but it changes the way
       |$fullRunnerName parses its command-line arguments in order to be compatible
       |with shebang scripts.
       |
       |When relying on the `run` sub-command, inputs and $baseRunnerName options can be mixed,
       |while program args have to be specified after `--`
       |  ${Console.BOLD}$progName [command] [${baseRunnerName}_options | input]... -- [program_arguments]...${Console.RESET}
       |
       |However, for the `shebang` sub-command, only a single input file can be set, while all $baseRunnerName options
       |have to be set before the input file.
       |All inputs after the first are treated as program arguments, without the need for `--`
       |  ${Console.BOLD}$progName shebang [${baseRunnerName}_options]... input [program_arguments]...${Console.RESET}
       |
       |Using this, it is possible to conveniently set up Unix shebang scripts. For example:
       |  ${ScalaCliConsole.GRAY}#!/usr/bin/env -S $progName shebang --scala-version 2.13
       |  println("Hello, world")${Console.RESET}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
