package scala.cli.commands

import caseapp._
import scala.build.BuildOptions

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

  def buildOptions: BuildOptions.ScalaJsOptions =
    BuildOptions.ScalaJsOptions(
      enable = js,
      version = jsVersion,
      mode = jsMode,
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps,
      dom = jsDom
    )
}
