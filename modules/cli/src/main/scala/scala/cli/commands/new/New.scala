package scala.cli.commands.`new`

import caseapp.core.RemainingArgs
import giter8.Giter8

import scala.build.Logger
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.HelpCommandGroup

object New extends ScalaCommand[NewOptions] {
  override def group: String = HelpCommandGroup.Main.toString

  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def runCommand(options: NewOptions, remainingArgs: RemainingArgs, logger: Logger): Unit =
    val inputArgs = remainingArgs.remaining
    Giter8.run(inputArgs.toArray)
    ()
}
