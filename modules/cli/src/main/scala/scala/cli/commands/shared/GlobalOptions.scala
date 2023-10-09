package scala.cli.commands.shared

import caseapp.*

import scala.cli.launcher.PowerOptions

case class GlobalOptions(
  @Recurse
  logging: LoggingOptions = LoggingOptions(),
  @Recurse
  globalSuppress: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions(),

  /** Duplication of [[scala.cli.launcher.LauncherOptions.powerOptions]]. Thanks to this, our unit
    * tests ensure that no subcommand defines an option that will clash with --power.
    */
  @Recurse
  powerOptions: PowerOptions = PowerOptions()
)

object GlobalOptions {
  implicit lazy val parser: Parser[GlobalOptions] = Parser.derive
  implicit lazy val help: Help[GlobalOptions]     = Help.derive

  lazy val default: GlobalOptions = GlobalOptions()

  def get(args: List[String]): Option[GlobalOptions] =
    parser
      .detailedParse(args, stopAtFirstUnrecognized = false, ignoreUnrecognized = true)
      .toOption
      .map(_._1)
}
