package scala.build.options

import bloop.config.{Config => BloopConfig}
import dependency._
import org.scalajs.linker.interface.{
  ESFeatures,
  ESVersion,
  ModuleKind,
  ModuleSplitStyle,
  Semantics,
  StandardConfig
}

import java.util.Locale

import scala.build.Logger
import scala.build.internal.Constants

final case class ScalaJsOptions(
  version: Option[String] = None,
  mode: Option[String] = None,
  moduleKindStr: Option[String] = None,
  checkIr: Option[Boolean] = None,
  emitSourceMaps: Boolean = false,
  dom: Option[Boolean] = None,
  header: Option[String] = None,
  allowBigIntsForLongs: Option[Boolean] = None,
  avoidClasses: Option[Boolean] = None,
  avoidLetsAndConsts: Option[Boolean] = None,
  moduleSplitStyleStr: Option[String] = None,
  esVersionStr: Option[String] = None
) {
  def platformSuffix: String =
    "sjs" + ScalaVersion.jsBinary(finalVersion).getOrElse(finalVersion)
  def jsDependencies(scalaVersion: String): Seq[AnyDependency] =
    if (scalaVersion.startsWith("2."))
      Seq(dep"org.scala-js::scalajs-library:$finalVersion")
    else
      Seq(dep"org.scala-js:scalajs-library_2.13:$finalVersion")
  def compilerPlugins(scalaVersion: String): Seq[AnyDependency] =
    if (scalaVersion.startsWith("2."))
      Seq(dep"org.scala-js:::scalajs-compiler:$finalVersion")
    else
      Nil

  def moduleKind(logger: Logger): ModuleKind =
    moduleKindStr.map(_.trim.toLowerCase(Locale.ROOT)).map {
      case "commonjs" | "common" => ModuleKind.CommonJSModule
      case "esmodule" | "es"     => ModuleKind.ESModule
      case "nomodule" | "none"   => ModuleKind.NoModule
      case unknown =>
        logger.message(
          s"Warning: unrecognized argument: $unknown for --js-module-kind parameter, use default value: nomodule"
        )
        ModuleKind.NoModule
    }.getOrElse(ModuleKind.NoModule)

  def moduleSplitStyle(logger: Logger): ModuleSplitStyle =
    moduleSplitStyleStr.map(_.trim.toLowerCase(Locale.ROOT)).map {
      case "fewestmodules"   => ModuleSplitStyle.FewestModules
      case "smallestmodules" => ModuleSplitStyle.SmallestModules
      case unknown =>
        logger.message(
          s"Warning: unrecognized argument: $unknown for --js-module-split-style parameter, use default value: fewestmodules"
        )
        ModuleSplitStyle.FewestModules
    }.getOrElse(ModuleSplitStyle.FewestModules)

  def esVersion(logger: Logger): ESVersion =
    esVersionStr.map(_.trim.toLowerCase(Locale.ROOT)).map {
      case "es5_1"  => ESVersion.ES5_1
      case "es2015" => ESVersion.ES2015
      case "es2016" => ESVersion.ES2016
      case "es2017" => ESVersion.ES2017
      case "es2018" => ESVersion.ES2018
      case "es2019" => ESVersion.ES2019
      case "es2020" => ESVersion.ES2020
      case "es2021" => ESVersion.ES2021
      case unknown =>
        val default = ESFeatures.Defaults.esVersion
        logger.message(
          s"Warning: unrecognized argument: $unknown for --js-es-version parameter, use default value: ${default.name}"
        )
        default
    }.getOrElse(ESFeatures.Defaults.esVersion)

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaJsVersion)

  private def configUnsafe(logger: Logger): BloopConfig.JsConfig = {
    val kind = moduleKind(logger) match {
      case ModuleKind.CommonJSModule => BloopConfig.ModuleKindJS.CommonJSModule
      case ModuleKind.ESModule       => BloopConfig.ModuleKindJS.ESModule
      case ModuleKind.NoModule       => BloopConfig.ModuleKindJS.NoModule
    }
    BloopConfig.JsConfig(
      version = finalVersion,
      mode =
        if (mode.contains("release")) BloopConfig.LinkerMode.Release
        else BloopConfig.LinkerMode.Debug,
      kind = kind,
      emitSourceMaps = emitSourceMaps,
      jsdom = dom,
      output = None,
      nodePath = None,
      toolchain = Nil
    )
  }

  def config(logger: Logger): BloopConfig.JsConfig =
    configUnsafe(logger)

  def linkerConfig(logger: Logger): StandardConfig = {
    var config = StandardConfig()

    config = config
      .withModuleKind(moduleKind(logger))
      .withModuleSplitStyle(moduleSplitStyle(logger))

    for (checkIr <- checkIr)
      config = config.withCheckIR(checkIr)

    val release   = mode.contains("release")
    val jsHeader0 = header.getOrElse("")

    val esFeatureDefaults = ESFeatures.Defaults
    val esFeature = ESFeatures.Defaults
      .withAllowBigIntsForLongs(
        allowBigIntsForLongs.getOrElse(esFeatureDefaults.allowBigIntsForLongs)
      )
      .withAvoidClasses(avoidClasses.getOrElse(esFeatureDefaults.avoidClasses))
      .withAvoidLetsAndConsts(avoidLetsAndConsts.getOrElse(esFeatureDefaults.avoidLetsAndConsts))
      .withESVersion(esVersion(logger))

    config = config
      .withSemantics(Semantics.Defaults)
      .withESFeatures(esFeature)
      .withOptimizer(release)
      .withParallel(true)
      .withSourceMap(emitSourceMaps)
      .withRelativizeSourceMapBase(None)
      .withClosureCompiler(release)
      .withPrettyPrint(false)
      .withBatchMode(true)
      .withJSHeader(jsHeader0)

    config
  }
}

object ScalaJsOptions {
  implicit val hasHashData: HasHashData[ScalaJsOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaJsOptions]     = ConfigMonoid.derive
}
