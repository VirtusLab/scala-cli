package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

// format: off
final case class LoggingOptions(
  @Recurse
    verbosityOptions: VerbosityOptions = VerbosityOptions(),
  @Group("Logging")
  @HelpMessage("Decrease verbosity")
  @Name("q")
    quiet: Boolean = false,
  @Group("Logging")
  @HelpMessage("Use progress bars")
    progress: Option[Boolean] = None
) {
  // format: on

  lazy val verbosity = verbosityOptions.verbosity - (if (quiet) 1 else 0)
}

object LoggingOptions {
  lazy val parser: Parser[LoggingOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[LoggingOptions, parser.D] = parser
  implicit lazy val help: Help[LoggingOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[LoggingOptions]       = JsonCodecMaker.make
}
