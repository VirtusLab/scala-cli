package scala.build.options

import bloop.config.{Config => BloopConfig}
import dependency._
import org.scalajs.linker.interface.{ESFeatures, ModuleKind, Semantics, StandardConfig}

import java.util.Locale

import scala.build.internal.Constants

final case class ScalaJsOptions(
  enable: Boolean = false,
  version: Option[String] = None,
  mode: Option[String] = None,
  moduleKindStr: Option[String] = None,
  checkIr: Option[Boolean] = None,
  emitSourceMaps: Boolean = false,
  dom: Option[Boolean] = None
) {
  def platformSuffix: Option[String] =
    if (enable) Some("sjs" + ScalaVersion.jsBinary(finalVersion).getOrElse(finalVersion))
    else None
  def jsDependencies: Seq[AnyDependency] =
    if (enable) Seq(dep"org.scala-js::scalajs-library:$finalVersion")
    else Nil
  def compilerPlugins: Seq[AnyDependency] =
    if (enable) Seq(dep"org.scala-js:::scalajs-compiler:$finalVersion")
    else Nil

  def orElse(other: ScalaJsOptions): ScalaJsOptions =
    ScalaJsOptions(
      enable = enable || other.enable,
      version = version.orElse(other.version),
      mode = mode.orElse(other.mode),
      moduleKindStr = moduleKindStr.orElse(other.moduleKindStr),
      checkIr = checkIr.orElse(other.checkIr),
      emitSourceMaps = emitSourceMaps || other.emitSourceMaps,
      dom = dom.orElse(other.dom)
    )

  private def moduleKind: ModuleKind =
    moduleKindStr.map(_.trim.toLowerCase(Locale.ROOT)).getOrElse("") match {
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

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaJsVersion)

  private def configUnsafe: BloopConfig.JsConfig = {
    val kind = moduleKind match {
      case ModuleKind.CommonJSModule => BloopConfig.ModuleKindJS.CommonJSModule
      case ModuleKind.ESModule       => BloopConfig.ModuleKindJS.ESModule
      case ModuleKind.NoModule       => BloopConfig.ModuleKindJS.NoModule
    }
    BloopConfig.JsConfig(
           version = finalVersion,
              mode = if (mode.contains("release")) BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
              kind = kind,
    emitSourceMaps = emitSourceMaps,
             jsdom = dom,
            output = None,
          nodePath = None,
         toolchain = Nil
    )
  }

  def config: Option[BloopConfig.JsConfig] =
    if (enable) Some(configUnsafe)
    else None

  def linkerConfig: StandardConfig = {
    var config = StandardConfig()

    config = config
      .withModuleKind(moduleKind)

    for (checkIr <- checkIr)
      config = config.withCheckIR(checkIr)

    val release = mode.contains("release")

    config = config
      .withSemantics(Semantics.Defaults)
      .withESFeatures(ESFeatures.Defaults)
      .withOptimizer(release)
      .withParallel(true)
      .withSourceMap(emitSourceMaps)
      .withRelativizeSourceMapBase(None)
      .withClosureCompiler(release)
      .withPrettyPrint(false)
      .withBatchMode(true)

    config
  }
}

object ScalaJsOptions {
  implicit val hasHashData: HasHashData[ScalaJsOptions] = {
    val underlying: HasHashData[ScalaJsOptions] = HasHashData.derive
    (prefix, t, update) =>
      if (t.enable)
        underlying.add(prefix, t, update)
  }
}
