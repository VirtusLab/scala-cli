package scala.cli.commands.bloop

import caseapp.*

import scala.cli.commands.shared.{GlobalSuppressWarningOptions, HasGlobalOptions, HelpMessages, LoggingOptions, SharedCompilationServerOptions}

// format: off
@HelpMessage(
  s"""Print Bloop output.
     |
     |${HelpMessages.bloopInfo}""".stripMargin)
final case class BloopOutputOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    globalSuppressWarning: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
) extends HasGlobalOptions
// format: on

object BloopOutputOptions {
  implicit lazy val parser: Parser[BloopOutputOptions] = Parser.derive
  implicit lazy val help: Help[BloopOutputOptions]   = Help.derive
}
