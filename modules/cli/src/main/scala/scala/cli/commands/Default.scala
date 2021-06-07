package scala.cli.commands

import caseapp._
import scala.cli.ScalaCli

object Default extends Command[RunOptions] {
  private[cli] var anyArgs = false
  def run(options: RunOptions, args: RemainingArgs): Unit =
    if (anyArgs)
      Run.run(options, args, defaultInputs = None)
    else {
      println(ScalaCli.help.help())
      sys.exit(0)
    }
}
