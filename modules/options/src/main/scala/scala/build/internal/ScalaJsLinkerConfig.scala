package scala.build.internal

final case class ScalaJsLinkerConfig(
  // trying to have the same defaults as org.scalajs.linker.interface.StandardConfig here
  moduleKind: String = ScalaJsLinkerConfig.ModuleKind.NoModule,
  checkIR: Boolean = false,
  sourceMap: Boolean = true,
  moduleSplitStyle: String = ScalaJsLinkerConfig.ModuleSplitStyle.FewestModules,
  smallModuleForPackage: List[String] = Nil,
  esFeatures: ScalaJsLinkerConfig.ESFeatures = ScalaJsLinkerConfig.ESFeatures(),
  jsHeader: Option[String] = None,
  prettyPrint: Boolean = false,
  relativizeSourceMapBase: Option[String] = None,
  remapEsModuleImportMap: Option[os.Path] = None
) {
  def linkerCliArgs: Seq[String] = {
    val moduleKindArgs       = Seq("--moduleKind", moduleKind)
    val moduleSplitStyleArgs = Seq("--moduleSplitStyle", moduleSplitStyle)
    val smallModuleForPackageArgs =
      if (smallModuleForPackage.nonEmpty)
        Seq("--smallModuleForPackages", smallModuleForPackage.mkString(","))
      else
        Nil
    val esFeaturesArgs = Seq("--esVersion", esFeatures.esVersion)
    val checkIRArgs    = if (checkIR) Seq("--checkIR") else Nil
    val sourceMapArgs  = if (sourceMap) Seq("--sourceMap") else Nil
    val relativizeSourceMapBaseArgs =
      relativizeSourceMapBase.toSeq
        .flatMap(uri => Seq("--relativizeSourceMap", uri))
    val prettyPrintArgs =
      if (prettyPrint) Seq("--prettyPrint")
      else Nil
    val jsHeaderArg = if (jsHeader.nonEmpty) Seq("--jsHeader", jsHeader.getOrElse("")) else Nil
    val jsEsModuleImportMap = if (remapEsModuleImportMap.nonEmpty)
      Seq("--importmap", remapEsModuleImportMap.getOrElse(os.pwd / "importmap.json").toString)
    else Nil

    val configArgs = Seq[os.Shellable](
      moduleKindArgs,
      moduleSplitStyleArgs,
      smallModuleForPackageArgs,
      esFeaturesArgs,
      checkIRArgs,
      sourceMapArgs,
      relativizeSourceMapBaseArgs,
      jsHeaderArg,
      prettyPrintArgs,
      jsEsModuleImportMap
    )

    configArgs.flatMap(_.value)
  }
}

object ScalaJsLinkerConfig {
  object ModuleKind {
    val NoModule       = "NoModule"
    val ESModule       = "ESModule"
    val CommonJSModule = "CommonJSModule"
  }

  object ModuleSplitStyle {
    val FewestModules   = "FewestModules"
    val SmallestModules = "SmallestModules"
    val SmallModulesFor = "SmallModulesFor"
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
}
