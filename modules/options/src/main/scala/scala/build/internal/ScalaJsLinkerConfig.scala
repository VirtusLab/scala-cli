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
  remapEsModuleImportMap: Option[os.Path] = None,
  emitWasm: Boolean = false
) {
  def linkerCliArgs: Seq[String] = {
    val moduleKindArgs            = Seq("--moduleKind", moduleKind)
    val moduleSplitStyleArgs      = Seq("--moduleSplitStyle", moduleSplitStyle)
    val smallModuleForPackageArgs =
      if (smallModuleForPackage.nonEmpty)
        Seq("--smallModuleForPackages", smallModuleForPackage.mkString(","))
      else
        Nil
    val esFeaturesArgs              = Seq("--esVersion", esFeatures.esVersion)
    val checkIRArgs                 = if (checkIR) Seq("--checkIR") else Nil
    val sourceMapArgs               = if (sourceMap) Seq("--sourceMap") else Nil
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
    val jsEmitWasm = if (emitWasm) Seq("--emitWasm") else Nil

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
      jsEsModuleImportMap,
      jsEmitWasm
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

  /** Scala.js ECMA Script versions, as spelled by `org.scalajs.linker.interface.ESVersion`.
    *
    * Only the values Scala CLI itself needs are named here. The set of versions actually supported
    * depends on the Scala.js version in use, which is resolved at link time, so Scala CLI does not
    * mirror the full list: it normalizes the spelling and forwards the value to the linker via
    * `--esVersion`, letting the linker be the authority on what it accepts.
    */
  object ESVersion {
    val ES5_1  = "ES5_1"
    val ES2015 = "ES2015"

    /** The oldest year-based version Scala.js supports, and therefore a fixed lower bound. */
    val minimumYear = 2015

    def default = ES2015
  }
}
