package scala.cli.commands.pgp

import caseapp.core.app.Command
import caseapp.core.help.Help
import caseapp.core.parser.Parser

import scala.build.Logger
import scala.build.input.{ScalaCliInvokeData, SubCommand}
import scala.cli.ScalaCli
import scala.cli.commands.RestrictableCommand
import scala.cli.commands.util.CommandHelpers
import scala.cli.internal.{CliLogger, ProcUtil}

abstract class PgpCommand[T](implicit myParser: Parser[T], help: Help[T])
    extends Command()(myParser, help)
    with CommandHelpers with RestrictableCommand[T] {
  override protected def invokeData: ScalaCliInvokeData =
    ScalaCliInvokeData(
      progName = ScalaCli.progName,
      subCommandName =
        name, // FIXME Should be the actual name that was called from the command line
      subCommand = SubCommand.Other,
      isShebangCapableShell = ProcUtil.isShebangCapableShell
    )

  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL

  override def shouldSuppressExperimentalFeatureWarnings: Boolean =
    false // TODO add handling for scala-cli-signing

  override def shouldSuppressDeprecatedFeatureWarnings: Boolean =
    false // TODO add handling for scala-cli-signing

  override def logger: Logger = CliLogger.default // TODO add handling for scala-cli-signing

  override def hidden = true
}
