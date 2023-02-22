package scala.cli.commands.shebang

import caseapp.RemainingArgs
import caseapp.core.help.HelpFormat

import scala.build.Logger
import scala.build.input.{ScalaCliInvokeData, SubCommand}
import scala.build.options.BuildOptions
import scala.cli.CurrentParams
import scala.cli.commands.run.Run
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.util.ArgHelpers.*

object Shebang extends ScalaCommand[ShebangOptions] {
  override def stopAtFirstUnrecognized: Boolean = true

  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.MUST
  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroups(Run.primaryHelpGroups)

  override def sharedOptions(options: ShebangOptions): Option[SharedOptions] =
    Run.sharedOptions(options.runOptions)

  override def invokeData: ScalaCliInvokeData =
    super.invokeData.copy(subCommand = SubCommand.Shebang)
  override def runCommand(options: ShebangOptions, args: RemainingArgs, logger: Logger): Unit =
    Run.runCommand(
      options.runOptions,
      args.remaining.headOption.toSeq,
      args.remaining.drop(1),
      () => None,
      logger,
      invokeData
    )
}
