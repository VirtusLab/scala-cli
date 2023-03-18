package scala.cli.commands.shared

import caseapp.*

case class GlobalOptions(
  @Recurse
  logging: LoggingOptions = LoggingOptions(),
  @Recurse
  globalSuppress: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions()
)

object GlobalOptions {
  implicit lazy val parser: Parser[GlobalOptions] = Parser.derive
  implicit lazy val help: Help[GlobalOptions]     = Help.derive

  def get(args: List[String]): Option[GlobalOptions] =
    parser
      .detailedParse(args, stopAtFirstUnrecognized = false, ignoreUnrecognized = true)
      .toOption
      .map(_._1)
}
