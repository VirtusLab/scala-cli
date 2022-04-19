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
  semantics: ScalaJsLinkerConfig.Semantics = ScalaJsLinkerConfig.Semantics()
) {
  def linkerCliArgs: Seq[String] = {

    // FIXME Fatal asInstanceOfs should be the default, but it seems we can't
    // pass Unchecked via the CLI here
    // It seems we can't pass the other semantics fields either.
    val semanticsArgs =
      if (semantics.asInstanceOfs == ScalaJsLinkerConfig.CheckedBehavior.Compliant)
        Seq("--compliantAsInstanceOfs")
      else
        Nil
    val moduleKindArgs       = Seq("--moduleKind", moduleKind)
    val moduleSplitStyleArgs = Seq("--moduleSplitStyle", moduleSplitStyle)
    val smallModuleForPackageArgs =
      if (smallModuleForPackage.nonEmpty)
        Seq("--smallModuleForPackages", smallModuleForPackage.mkString(","))
      else
        Nil
    val esFeaturesArgs =
      if (esFeatures.esVersion == ScalaJsLinkerConfig.ESVersion.ES2015)
        Seq("--es2015")
      else
        Nil
    val checkIRArgs   = if (checkIR) Seq("--checkIR") else Nil
    val sourceMapArgs = if (sourceMap) Seq("--sourceMap") else Nil
    val relativizeSourceMapBaseArgs =
      relativizeSourceMapBase.toSeq
        .flatMap(uri => Seq("--relativizeSourceMap", uri))
    val prettyPrintArgs =
      if (prettyPrint) Seq("--prettyPrint")
      else Nil
    val jsHeaderArg = if (jsHeader.nonEmpty) Seq("--jsHeader", jsHeader.getOrElse("")) else Nil
    val configArgs = Seq[os.Shellable](
      semanticsArgs,
      moduleKindArgs,
      moduleSplitStyleArgs,
      smallModuleForPackageArgs,
      esFeaturesArgs,
      checkIRArgs,
      sourceMapArgs,
      relativizeSourceMapBaseArgs,
      jsHeaderArg,
      prettyPrintArgs
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

  final case class Semantics(
    asInstanceOfs: String = CheckedBehavior.Compliant
  )

  object CheckedBehavior {
    val Compliant = "Compliant"
  }
}
