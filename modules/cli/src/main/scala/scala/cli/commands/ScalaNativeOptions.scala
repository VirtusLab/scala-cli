package scala.cli.commands

import caseapp._
import upickle.default.{ReadWriter => RW, macroRW}

import scala.build.options

// format: off
final case class ScalaNativeOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala Native")
    native: Boolean = false,

  @Group("Scala Native")
    nativeVersion: Option[String] = None,
  @Group("Scala Native")
    nativeMode: Option[String] = None,
  @Group("Scala Native")
    nativeGc: Option[String] = None,

  @Group("Scala Native")
    nativeClang: Option[String] = None,
  @Group("Scala Native")
    nativeClangpp: Option[String] = None,

  @Group("Scala Native")
    nativeLinking: List[String] = Nil,
  @Group("Scala Native")
    nativeLinkingDefaults: Option[Boolean] = None,

  @Group("Scala Native")
    nativeCompile: List[String] = Nil,
  @Group("Scala Native")
    nativeCompileDefaults: Option[Boolean] = None

) {
  // format: on

  def buildOptions: options.ScalaNativeOptions =
    options.ScalaNativeOptions(
      nativeVersion,
      nativeMode,
      nativeGc,
      nativeClang,
      nativeClangpp,
      nativeLinking,
      nativeLinkingDefaults,
      nativeCompile,
      nativeCompileDefaults
    )

}

object ScalaNativeOptions {
  lazy val parser: Parser[ScalaNativeOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[ScalaNativeOptions, parser.D] = parser
  implicit lazy val help: Help[ScalaNativeOptions]                      = Help.derive
  implicit lazy val rw: RW[ScalaNativeOptions] = macroRW
}
