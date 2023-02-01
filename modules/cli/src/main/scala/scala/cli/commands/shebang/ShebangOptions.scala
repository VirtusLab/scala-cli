package scala.cli.commands.shebang

import caseapp.*

import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName, progName}
import scala.cli.commands.run.RunOptions
import scala.cli.commands.shared.{HasSharedOptions, SharedOptions}

@HelpMessage(
  s"""|Like `run`, but handier for shebang scripts.
      |
      |This command is equivalent to the `run` sub-command, but it changes the way
      |$fullRunnerName parses its command-line arguments in order to be compatible
      |with shebang scripts.
      |
      |When relying on the `run` sub-command, inputs and $baseRunnerName options can be mixed,
      |while program args have to be specified after `--`
      |
      |```sh
      |$progName [command] [${baseRunnerName}_options | input]... -- [program_arguments]...
      |```
      |
      |However, for the `shebang` sub-command, only a single input file can be set, while all $baseRunnerName options
      |have to be set before the input file.
      |All inputs after the first are treated as program arguments, without the need for `--`
      |```sh
      |$progName shebang [${baseRunnerName}_options]... input [program_arguments]...
      |```
      |
      |Using this, it is possible to conveniently set up Unix shebang scripts. For example:
      |```sh
      |#!/usr/bin/env -S $progName shebang --scala-version 2.13
      |println("Hello, world")
      |```
      |
      |""".stripMargin
)
final case class ShebangOptions(
  @Recurse
  runOptions: RunOptions = RunOptions()
) extends HasSharedOptions {
  override def shared: SharedOptions = runOptions.shared
}

object ShebangOptions {
  implicit lazy val parser: Parser[ShebangOptions] = Parser.derive
  implicit lazy val help: Help[ShebangOptions]     = Help.derive
}
