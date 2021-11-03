package scala.cli.commands

import caseapp._
import upickle.default.{ReadWriter, macroRW}

import scala.build.options

// format: off
final case class ScalaJsOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala.JS")
    js: Boolean = false,

  @Group("Scala.JS")
  @HelpMessage("The Scala JS version")
    jsVersion: Option[String] = None,
  @Group("Scala.JS")
  @HelpMessage("The Scala JS mode, either `dev` or `release`")
    jsMode: Option[String] = None,
  @HelpMessage("The Scala JS module kind: commonjs/common, esmodule/es, nomodule/none")
  @Group("Scala.JS")
    jsModuleKind: Option[String] = None,

  @Group("Scala.JS")
  jsCheckIr: Option[Boolean] = None,

  @Group("Scala.JS")
  @HelpMessage("Emit source maps")
    jsEmitSourceMaps: Boolean = false,
  @Group("Scala.JS")
  @HelpMessage("Enable jsdom")
    jsDom: Option[Boolean] = None

) {
  // format: on

  def buildOptions: options.ScalaJsOptions =
    options.ScalaJsOptions(
      version = jsVersion,
      mode = jsMode,
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps,
      dom = jsDom
    )
}

object ScalaJsOptions {
  lazy val parser: Parser[ScalaJsOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[ScalaJsOptions, parser.D] = parser
  implicit lazy val help: Help[ScalaJsOptions]                      = Help.derive
  implicit lazy val jsonCodec: ReadWriter[ScalaJsOptions]           = macroRW
}
