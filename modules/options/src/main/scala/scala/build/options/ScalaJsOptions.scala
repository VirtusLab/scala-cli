package scala.build.options

import bloop.config.{Config => BloopConfig}
import dependency._

import java.util.Locale

import scala.build.Logger
import scala.build.internal.{Constants, ScalaJsLinkerConfig}

final case class ScalaJsOptions(
  version: Option[String] = None,
  mode: Option[String] = None,
  moduleKindStr: Option[String] = None,
  checkIr: Option[Boolean] = None,
  emitSourceMaps: Boolean = false,
  sourceMapsDest: Option[os.Path] = None,
  dom: Option[Boolean] = None,
  header: Option[String] = None,
  allowBigIntsForLongs: Option[Boolean] = None,
  avoidClasses: Option[Boolean] = None,
  avoidLetsAndConsts: Option[Boolean] = None,
  moduleSplitStyleStr: Option[String] = None,
  esVersionStr: Option[String] = None,
  fullOpt: Option[Boolean] = None,
  noOpt: Option[Boolean] = None
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

  def moduleKind(logger: Logger): String =
    moduleKindStr
      .map(_.trim.toLowerCase(Locale.ROOT))
      .map {
        case "commonjs" | "common" => ScalaJsLinkerConfig.ModuleKind.CommonJSModule
        case "esmodule" | "es"     => ScalaJsLinkerConfig.ModuleKind.ESModule
        case "nomodule" | "none"   => ScalaJsLinkerConfig.ModuleKind.NoModule
        case unknown =>
          logger.message(
            s"Warning: unrecognized argument: $unknown for --js-module-kind parameter, using default value: nomodule"
          )
          ScalaJsLinkerConfig.ModuleKind.NoModule
      }
      .getOrElse(ScalaJsLinkerConfig.ModuleKind.NoModule)

  def moduleSplitStyle(logger: Logger): String =
    moduleSplitStyleStr
      .map(_.trim.toLowerCase(Locale.ROOT))
      .map {
        case "fewestmodules"   => ScalaJsLinkerConfig.ModuleSplitStyle.FewestModules
        case "smallestmodules" => ScalaJsLinkerConfig.ModuleSplitStyle.SmallestModules
        case unknown =>
          logger.message(
            s"Warning: unrecognized argument: $unknown for --js-module-split-style parameter, use default value: fewestmodules"
          )
          ScalaJsLinkerConfig.ModuleSplitStyle.FewestModules
      }
      .getOrElse(ScalaJsLinkerConfig.ModuleSplitStyle.FewestModules)

  def esVersion(logger: Logger): String =
    esVersionStr
      .map(_.trim.toLowerCase(Locale.ROOT))
      .map {
        case "es5_1"  => ScalaJsLinkerConfig.ESVersion.ES5_1
        case "es2015" => ScalaJsLinkerConfig.ESVersion.ES2015
        case "es2016" => ScalaJsLinkerConfig.ESVersion.ES2016
        case "es2017" => ScalaJsLinkerConfig.ESVersion.ES2017
        case "es2018" => ScalaJsLinkerConfig.ESVersion.ES2018
        case "es2019" => ScalaJsLinkerConfig.ESVersion.ES2019
        case "es2020" => ScalaJsLinkerConfig.ESVersion.ES2020
        case "es2021" => ScalaJsLinkerConfig.ESVersion.ES2021
        case unknown =>
          val default = ScalaJsLinkerConfig.ESVersion.default
          logger.message(
            s"Warning: unrecognized argument: $unknown for --js-es-version parameter, use default value: $default"
          )
          default
      }
      .getOrElse(ScalaJsLinkerConfig.ESVersion.default)

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaJsVersion)

  private def configUnsafe(logger: Logger): BloopConfig.JsConfig = {
    val kind = moduleKind(logger) match {
      case ScalaJsLinkerConfig.ModuleKind.CommonJSModule => BloopConfig.ModuleKindJS.CommonJSModule
      case ScalaJsLinkerConfig.ModuleKind.ESModule       => BloopConfig.ModuleKindJS.ESModule
      case ScalaJsLinkerConfig.ModuleKind.NoModule       => BloopConfig.ModuleKindJS.NoModule
      // shouldn't happen
      case _ => BloopConfig.ModuleKindJS.NoModule
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

  def linkerConfig(logger: Logger): ScalaJsLinkerConfig = {
    val esFeatureDefaults = ScalaJsLinkerConfig.ESFeatures()
    val esFeatures = ScalaJsLinkerConfig.ESFeatures(
      allowBigIntsForLongs =
        allowBigIntsForLongs.getOrElse(esFeatureDefaults.allowBigIntsForLongs),
      avoidClasses = avoidClasses.getOrElse(esFeatureDefaults.avoidClasses),
      avoidLetsAndConsts = avoidLetsAndConsts.getOrElse(esFeatureDefaults.avoidLetsAndConsts),
      esVersion = esVersion(logger)
    )

    ScalaJsLinkerConfig(
      moduleKind(logger),
      checkIr.getOrElse(false), // meh
      emitSourceMaps,
      moduleSplitStyle(logger),
      esFeatures,
      header
    )
  }
}

object ScalaJsOptions {
  implicit val hasHashData: HasHashData[ScalaJsOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaJsOptions]     = ConfigMonoid.derive
}
