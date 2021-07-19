package scala.build.options

import _root_.bloop.config.{Config => BloopConfig}
import dependency._

import java.nio.file.Paths

import scala.build.internal.Constants
import scala.scalanative.{build => sn}

final case class ScalaNativeOptions(
  enable: Boolean = false,
  version: Option[String] = None,
  modeStr: Option[String] = None,
  gcStr: Option[String] = None,
  clang: Option[String] = None,
  clangpp: Option[String] = None,
  linkingOptions: List[String] = Nil,
  linkingDefaults: Boolean = true,
  compileOptions: List[String] = Nil,
  compileDefaults: Boolean = true
) {

  def nativeWorkDir(root: os.Path, projectName: String): os.Path =
    root / ".scala" / projectName / "native"

  def orElse(other: ScalaNativeOptions): ScalaNativeOptions =
    ScalaNativeOptions(
      enable = enable || other.enable,
      version = version.orElse(other.version),
      modeStr = modeStr.orElse(other.modeStr),
      gcStr = gcStr.orElse(other.gcStr),
      clang = clang.orElse(other.clang),
      clangpp = clangpp.orElse(other.clangpp),
      linkingOptions = linkingOptions ++ other.linkingOptions,
      linkingDefaults = linkingDefaults || other.linkingDefaults,
      compileOptions = compileOptions ++ other.compileOptions,
      compileDefaults = compileDefaults || other.compileDefaults
    )

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaNativeVersion)

  private def gc: sn.GC =
    gcStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.GC.default
      case Some(other) => sn.GC(other)
    }
  private def mode: sn.Mode =
    modeStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.Discover.mode()
      case Some(other) => sn.Mode(other)
    }

  private def clangPath = clang.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clang())
  private def clangppPath = clangpp.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clangpp())
  private def finalLinkingOptions =
    linkingOptions ++ (if (linkingDefaults) sn.Discover.linkingOptions() else Nil)
  private def finalCompileOptions =
    compileOptions ++ (if (compileDefaults) sn.Discover.compileOptions() else Nil)

  def platformSuffix: Option[String] =
    if (enable) Some("native" + ScalaVersion.nativeBinary(finalVersion).getOrElse(finalVersion))
    else None
  def nativeDependencies: Seq[AnyDependency] =
    if (enable)
      Seq("nativelib", "javalib", "auxlib", "scalalib")
        .map(name => dep"org.scala-native::$name::$finalVersion")
    else
      Nil
  def compilerPlugins: Seq[AnyDependency] =
    if (enable) Seq(dep"org.scala-native:::nscplugin:$finalVersion")
    else Nil

  private def bloopConfigUnsafe: BloopConfig.NativeConfig =
    BloopConfig.NativeConfig(
           version = finalVersion,
                     // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
              mode = if (mode.name == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
                gc = gc.name,
      targetTriple = None,
             clang = clangPath,
           clangpp = clangppPath,
         toolchain = Nil,
           options = BloopConfig.NativeOptions(
               linker = finalLinkingOptions,
             compiler = finalCompileOptions
           ),
         linkStubs = false,
             check = false,
              dump = false,
            output = None
    )

  def bloopConfig: Option[BloopConfig.NativeConfig] =
    if (enable) Some(bloopConfigUnsafe)
    else None

  private def configUnsafe: sn.NativeConfig =
    sn.NativeConfig.empty
      .withGC(gc)
      .withMode(mode)
      .withLinkStubs(false)
      .withClang(clangPath)
      .withClangPP(clangppPath)
      .withLinkingOptions(linkingOptions)
      .withCompileOptions(compileOptions)

  def config: Option[sn.NativeConfig] =
    if (enable) Some(configUnsafe)
    else None

}

object ScalaNativeOptions {
  implicit val hasHashData: HasHashData[ScalaNativeOptions] = {
    val underlying: HasHashData[ScalaNativeOptions] = HasHashData.derive
    (prefix, t, update) =>
      if (t.enable)
        underlying.add(prefix, t, update)
  }
}
