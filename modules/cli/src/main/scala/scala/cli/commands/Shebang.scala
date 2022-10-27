package scala.cli.commands

import caseapp.RemainingArgs

import scala.build.Logger
import scala.build.options.BuildOptions
import scala.cli.CurrentParams

object Shebang extends ScalaCommand[ShebangOptions] {
  override def stopAtFirstUnrecognized: Boolean = true
  override def sharedOptions(options: ShebangOptions): Option[SharedOptions] =
    Run.sharedOptions(options.runOptions)
  override def runCommand(options: ShebangOptions, args: RemainingArgs, logger: Logger): Unit =
    Run.runCommand(
      options.runOptions,
      args.remaining.headOption.toSeq,
      args.remaining.drop(1),
      () => None,
      logger
    )
}
