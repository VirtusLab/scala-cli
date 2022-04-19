package scala.cli.commands

import caseapp._

@HelpMessage(
  """|Like `run`, but more handy from shebang scripts
     |
     |This command is equivalent to `run`, but it changes the way
     |`scala-cli` parses its command-line arguments in order to be compatible
     |with shebang scripts.
     |
     |Normally, inputs and scala-cli options can be mixed. Program have to be specified after `--`
     |
     |```sh
     |scala-cli [command] [scala_cli_options | input]... -- [program_arguments]...
     |```
     |
     |Contrary, for shebang command, only a single input file can be set, all scala-cli options
     |have to be set before the input file, and program arguments after the input file
     |```sh
     |scala-cli shebang [scala_cli_options]... input [program_arguments]...
     |```
     |
     |Using this, it is possible to conveniently set up Unix shebang scripts. For example:
     |```sh
     |#!/usr/bin/env -S scala-cli shebang --scala-version 2.13
     |println("Hello, world)
     |```
     |
     |""".stripMargin
)
final case class ShebangOptions(
  @Recurse
  runOptions: RunOptions = RunOptions()
)

object ShebangOptions {
  implicit lazy val parser: Parser[ShebangOptions] = Parser.derive
  implicit lazy val help: Help[ShebangOptions]     = Help.derive
}
