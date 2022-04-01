package scala.cli.commands.pgp

import caseapp._

import scala.cli.commands.util.CommandHelpers

abstract class ExternalCommand extends Command[ExternalCommand.DummyOptions] with CommandHelpers {
  override def hasHelp                 = false
  override def stopAtFirstUnrecognized = true

  def actualHelp: Help[_]

  def run(options: ExternalCommand.DummyOptions, args: RemainingArgs): Unit = {
    val unparsedPart =
      if (args.unparsed.isEmpty) Nil
      else Seq("--") ++ args.unparsed
    val allArgs = args.remaining ++ unparsedPart
    run(allArgs)
  }

  def run(args: Seq[String]): Unit
}

object ExternalCommand {

  final case class DummyOptions()

  object DummyOptions {
    implicit lazy val parser: Parser[DummyOptions] = Parser.derive
    implicit lazy val help: Help[DummyOptions]     = Help.derive
  }

}
