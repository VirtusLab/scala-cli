package scala.cli.commands

import caseapp._

import scala.build.options

// format: off
final case class ScalaJsOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala.JS")
    js: Boolean = false,

  @Group("Scala.JS")
    jsVersion: Option[String] = None,
  @Group("Scala.JS")
    jsMode: Option[String] = None,
  @Group("Scala.JS")
    jsModuleKind: Option[String] = None,

  jsCheckIr: Option[Boolean] = None,

  @Group("Scala.JS")
    jsEmitSourceMaps: Boolean = false,
  @Group("Scala.JS")
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
  implicit lazy val parser                     = Parser[ScalaJsOptions]
  implicit lazy val help: Help[ScalaJsOptions] = Help.derive
}
