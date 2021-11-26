package scala.build.options

import _root_.bloop.config.{Config => BloopConfig}
import dependency._

import java.nio.file.Paths

import scala.build.internal.Constants
import scala.scalanative.{build => sn}

final case class ScalaNativeOptions(
  version: Option[String] = None,
  modeStr: Option[String] = None,
  gcStr: Option[String] = None,
  clang: Option[String] = None,
  clangpp: Option[String] = None,
  linkingOptions: List[String] = Nil,
  linkingDefaults: Option[Boolean] = None,
  compileOptions: List[String] = Nil,
  compileDefaults: Option[Boolean] = None
) {

  def nativeWorkDir(root: os.Path, projectName: String): os.Path =
    root / ".scala" / projectName / "native"

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaNativeVersion)

  private def gc(): sn.GC =
    gcStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.GC.default
      case Some(other)            => sn.GC(other)
    }
  private def mode(): sn.Mode =
    modeStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.Discover.mode()
      case Some(other)            => sn.Mode(other)
    }

  private def clangPath() = clang
    .filter(_.nonEmpty)
    .map(Paths.get(_))
    .getOrElse(sn.Discover.clang())
  private def clangppPath() = clangpp
    .filter(_.nonEmpty)
    .map(Paths.get(_))
    .getOrElse(sn.Discover.clangpp())
  private def finalLinkingOptions() =
    linkingOptions ++ (if (linkingDefaults.getOrElse(true)) sn.Discover.linkingOptions() else Nil)
  private def finalCompileOptions() =
    compileOptions ++ (if (compileDefaults.getOrElse(true)) sn.Discover.compileOptions() else Nil)

  def platformSuffix: String =
    "native" + ScalaVersion.nativeBinary(finalVersion).getOrElse(finalVersion)
  def nativeDependencies: Seq[AnyDependency] =
    Seq("nativelib", "javalib", "auxlib", "scalalib")
      .map(name => dep"org.scala-native::$name::$finalVersion")
  def compilerPlugins: Seq[AnyDependency] =
    Seq(dep"org.scala-native:::nscplugin:$finalVersion")

  def bloopConfig(): BloopConfig.NativeConfig =
    BloopConfig.NativeConfig(
      version = finalVersion,
      // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
      mode =
        if (mode().name == "release") BloopConfig.LinkerMode.Release
        else BloopConfig.LinkerMode.Debug,
      gc = gc().name,
      targetTriple = None,
      clang = clangPath(),
      clangpp = clangppPath(),
      toolchain = Nil,
      options = BloopConfig.NativeOptions(
        linker = finalLinkingOptions(),
        compiler = finalCompileOptions()
      ),
      linkStubs = false,
      check = false,
      dump = false,
      output = None
    )

  def config(): sn.NativeConfig =
    sn.NativeConfig.empty
      .withGC(gc())
      .withMode(mode())
      .withLinkStubs(false)
      .withClang(clangPath())
      .withClangPP(clangppPath())
      .withLinkingOptions(linkingOptions)
      .withCompileOptions(compileOptions)

}

object ScalaNativeOptions {
  implicit val hasHashData: HasHashData[ScalaNativeOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaNativeOptions]     = ConfigMonoid.derive
}
