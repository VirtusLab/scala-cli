package scala.cli.commands

import caseapp._

object Default extends CaseApp[RunOptions] {
  def run(options: RunOptions, args: RemainingArgs): Unit =
    Run.run(options, args, defaultInputs = None)
}
