package scala.cli.commands.pgp

import caseapp._

import scala.cli.commands.util.CommandHelpers

abstract class ExternalCommand extends Command[DummyOptions] with CommandHelpers {
  override def hasHelp                 = false
  override def stopAtFirstUnrecognized = true

  def actualHelp: Help[_]

  def run(options: DummyOptions, args: RemainingArgs): Unit = {
    val unparsedPart =
      if (args.unparsed.isEmpty) Nil
      else Seq("--") ++ args.unparsed
    val allArgs = args.remaining ++ unparsedPart
    run(allArgs)
  }

  def run(args: Seq[String]): Unit
}
