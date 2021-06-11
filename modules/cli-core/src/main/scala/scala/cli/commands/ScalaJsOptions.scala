package scala.cli.commands

import java.util.Locale

import bloop.config.{Config => BloopConfig}
import caseapp._
import scala.build.{Build, Project}
import scala.build.internal.Constants
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.linker.interface.Semantics
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.ESFeatures

final case class ScalaJsOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala.JS")
    js: Boolean = false,

  @Group("Scala.JS")
    jsVersion: Option[String] = None,
  @Group("Scala.JS")
    jsMode: String = "debug",
  @Group("Scala.JS")
    jsModuleKind: Option[String] = None,

  jsCheckIr: Option[Boolean] = None,

  @Group("Scala.JS")
    jsEmitSourceMaps: Boolean = false,
  @Group("Scala.JS")
    jsDom: Option[Boolean] = None

) {

  def finalVersion = jsVersion.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaJsVersion)

  private def moduleKind: ModuleKind =
    jsModuleKind.map(_.trim.toLowerCase(Locale.ROOT)).getOrElse("") match {
      case "commonjs" | "common" => ModuleKind.CommonJSModule
      case "esmodule" | "es"     => ModuleKind.ESModule
      case "nomodule" | "none"   => ModuleKind.NoModule
      case _                     => ModuleKind.CommonJSModule
    }
  private def moduleKindName: String =
    moduleKind match {
      case ModuleKind.CommonJSModule => "commonjs"
      case ModuleKind.ESModule => "esmodule"
      case ModuleKind.NoModule => "nomodule"
    }

  def buildOptions: Option[Build.ScalaJsOptions] =
    if (js) Some(Build.scalaJsOptions(bloopConfig))
    else None

  def bloopConfig: BloopConfig.JsConfig = {
    val kind = moduleKind match {
      case ModuleKind.CommonJSModule => BloopConfig.ModuleKindJS.CommonJSModule
      case ModuleKind.ESModule       => BloopConfig.ModuleKindJS.ESModule
      case ModuleKind.NoModule       => BloopConfig.ModuleKindJS.NoModule
    }
    BloopConfig.JsConfig(
           version = finalVersion,
              mode = if (jsMode == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
              kind = kind,
    emitSourceMaps = jsEmitSourceMaps,
             jsdom = jsDom,
            output = None,
          nodePath = None,
         toolchain = Nil
    )
  }

  def config: StandardConfig = {
    var config = StandardConfig()

    config = config
      .withModuleKind(moduleKind)

    for (checkIr <- jsCheckIr)
      config = config.withCheckIR(checkIr)

    val release = jsMode == "release"

    config = config
      .withSemantics(Semantics.Defaults)
      .withESFeatures(ESFeatures.Defaults)
      .withOptimizer(release)
      .withParallel(true)
      .withSourceMap(jsEmitSourceMaps)
      .withRelativizeSourceMapBase(None)
      .withClosureCompiler(release)
      .withPrettyPrint(false)
      .withBatchMode(true)

    config
  }

}
