package scala.build.internal

final case class ScalaJsLinkerConfig(
  // trying to have the same defaults as org.scalajs.linker.interface.StandardConfig here
  moduleKind: String = ScalaJsLinkerConfig.ModuleKind.NoModule,
  checkIR: Boolean = false,
  sourceMap: Boolean = true,
  moduleSplitStyle: String = ScalaJsLinkerConfig.ModuleSplitStyle.FewestModules,
  esFeatures: ScalaJsLinkerConfig.ESFeatures = ScalaJsLinkerConfig.ESFeatures(),
  jsHeader: Option[String] = None,
  prettyPrint: Boolean = false,
  relativizeSourceMapBase: Option[String] = None,
  semantics: ScalaJsLinkerConfig.Semantics = ScalaJsLinkerConfig.Semantics()
)

object ScalaJsLinkerConfig {
  object ModuleKind {
    val NoModule       = "NoModule"
    val ESModule       = "ESModule"
    val CommonJSModule = "CommonJSModule"
  }

  object ModuleSplitStyle {
    val FewestModules   = "FewestModules"
    val SmallestModules = "SmallestModules"
  }

  final case class ESFeatures(
    allowBigIntsForLongs: Boolean = false,
    avoidClasses: Boolean = true,
    avoidLetsAndConsts: Boolean = true,
    esVersion: String = ESVersion.default
  )

  object ESVersion {
    val ES5_1  = "ES5_1"
    val ES2015 = "ES2015"
    val ES2016 = "ES2016"
    val ES2017 = "ES2017"
    val ES2018 = "ES2018"
    val ES2019 = "ES2019"
    val ES2020 = "ES2020"
    val ES2021 = "ES2021"

    def default = ES2015
  }

  final case class Semantics(
    asInstanceOfs: String = CheckedBehavior.Compliant
  )

  object CheckedBehavior {
    val Compliant = "Compliant"
  }
}
