package scala.cli.commands

import caseapp._
import upickle.default.{ReadWriter => RW, _}

// format: off
final case class VerbosityOptions(
  @Group("Logging")
  @HelpMessage("Increase verbosity (can be specified multiple times)")
  @Name("v")
    verbose: Int @@ Counter = Tag.of(0)
) {
  // format: on

  lazy val verbosity = Tag.unwrap(verbose)

}

object VerbosityOptions {
  lazy val parser: Parser[VerbosityOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[VerbosityOptions, parser.D] = parser
  implicit lazy val help: Help[VerbosityOptions]                      = Help.derive
  implicit val rwCounter: RW[Int @@ Counter] = readwriter[ujson.Value].bimap[Int @@ Counter](
    x => ujson.Num(Tag.unwrap(x)),
    json => Tag.of(json.num.toInt)
  )
  implicit val rw: RW[VerbosityOptions] = macroRW
}
