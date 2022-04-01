package scala.cli.commands.pgp

import caseapp._

import scala.cli.commands.util.CommandHelpers

abstract class ExternalCommand extends Command[ExternalCommandOptions] with CommandHelpers {
  override def hasHelp                 = false
  override def stopAtFirstUnrecognized = true

  def actualHelp: Help[_]

  def run(options: ExternalCommandOptions, args: RemainingArgs): Unit = {
    val unparsedPart =
      if (args.unparsed.isEmpty) Nil
      else Seq("--") ++ args.unparsed
    val allArgs = args.remaining ++ unparsedPart
    run(allArgs)
  }

  def run(args: Seq[String]): Unit
}
