package scala.build.options

import _root_.bloop.config.{Config => BloopConfig}
import dependency._

import java.nio.file.Paths

import scala.build.internal.Constants
import scala.scalanative.build.LTO
import scala.scalanative.{build => sn}

enum ScalaNativeTarget:
  case Application, LibraryDynamic, LibraryStatic

  def toBuildTarget: sn.BuildTarget =
    this match
      case Application    => sn.BuildTarget.application
      case LibraryDynamic => sn.BuildTarget.libraryDynamic
      case libraryStatic  => sn.BuildTarget.libraryStatic

object ScalaNativeTarget:
  import ScalaNativeTarget.*
  def fromString(str: String): Option[ScalaNativeTarget] =
    str match
      case "application" | "app"                    => Some(Application)
      case "library-dynamic" | "dynamic" | "shared" => Some(LibraryDynamic)
      case "library-static" | "static"              => Some(LibraryStatic)
      case _                                        => None

final case class ScalaNativeOptions(
  version: Option[String] = None,
  modeStr: Option[String] = None,
  ltoStr: Option[String] = None,
  gcStr: Option[String] = None,
  clang: Option[String] = None,
  clangpp: Option[String] = None,
  linkingOptions: List[String] = Nil,
  linkingDefaults: Option[Boolean] = None,
  compileOptions: List[String] = Nil,
  compileDefaults: Option[Boolean] = None,
  embedResources: Option[Boolean] = None,
  buildTargetStr: Option[String] = None
) {

  def finalVersion = version.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaNativeVersion)

  def numeralVersion = SNNumeralVersion.parse(finalVersion)

  def target(): Option[ScalaNativeTarget] =
    buildTargetStr.flatMap(ScalaNativeTarget.fromString)

  private def targetCliOption(): List[String] =
    import ScalaNativeTarget.*
    val targ = target().map {
      case Application    => "application"
      case LibraryDynamic => "library-dynamic"
      case LibraryStatic  => "library-static"
    }

    targ.toList.flatMap(opt => List("--build-target", opt))

  private def gc(): sn.GC =
    gcStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.Discover.GC()
      case Some(other)            => sn.GC(other)
    }
  private def gcCliOption(): List[String] =
    List("--gc", gc().name)

  private def mode(): sn.Mode =
    modeStr.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.Discover.mode()
      case Some(other)            => sn.Mode(other)
    }
  private def modeCliOption(): List[String] =
    List("--mode", mode().name)

  private def clangPath() = clang
    .filter(_.nonEmpty)
    .map(Paths.get(_))
    .getOrElse(sn.Discover.clang())
  private def clangCliOption(): List[String] =
    List("--clang", clangPath().toString())

  private def clangppPath() = clangpp
    .filter(_.nonEmpty)
    .map(Paths.get(_))
    .getOrElse(sn.Discover.clangpp())
  private def clangppCliOption(): List[String] =
    List("--clang-pp", clangppPath().toString())

  private def finalLinkingOptions(): List[String] =
    linkingOptions ++ (if (linkingDefaults.getOrElse(true)) sn.Discover.linkingOptions() else Nil)
  private def finalCompileOptions(): List[String] =
    compileOptions ++ (if (compileDefaults.getOrElse(true)) sn.Discover.compileOptions() else Nil)

  private def linkingCliOptions(): List[String] =
    finalLinkingOptions().flatMap(option => List("--linking-option", option))

  private def compileCliOptions(): List[String] =
    finalCompileOptions().flatMap(option => List("--compile-option", option))
  private def ltoOptions(): List[String] =
    ltoStr.map(_.trim).filter(_.nonEmpty)
      .map(lto => LTO.apply(lto))
      .map(lto => List("--lto", lto.name)).getOrElse(Nil)
  private def resourcesCliOptions(resourcesExist: Boolean): List[String] =
    if (embedResources.getOrElse(true))
      (numeralVersion, resourcesExist) match {
        case (Some(numeralVersion), true) if numeralVersion >= SNNumeralVersion(0, 4, 4) =>
          List("--embed-resources")
        case _ => Nil
      }
    else Nil

  def platformSuffix: String =
    "native" + ScalaVersion.nativeBinary(finalVersion).getOrElse(finalVersion)

  def nativeDependencies(scalaVersion: String): Seq[AnyDependency] = {
    // https://github.com/scala-native/scala-native/pull/3326
    val scalalibVersion =
      if (finalVersion.startsWith("0.4.")) finalVersion
      else s"$scalaVersion+$finalVersion"
    // Since 0.5.x Scala Native requires explicit dependency on javalib
    // See https://github.com/scala-native/scala-native/pull/3566
    val javalib = dep"org.scala-native::javalib::$finalVersion"
    if (scalaVersion.startsWith("2."))
      Seq(dep"org.scala-native::scalalib::$scalalibVersion", javalib)
    else
      Seq(dep"org.scala-native::scala3lib::$scalalibVersion", javalib)
  }

  def compilerPlugins: Seq[AnyDependency] =
    Seq(dep"org.scala-native:::nscplugin:$finalVersion")

  def bloopConfig(): BloopConfig.NativeConfig =
    BloopConfig.NativeConfig(
      version = finalVersion,
      // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
      mode =
        if (mode() == sn.Mode.releaseFast || mode() == sn.Mode.releaseFull)
          BloopConfig.LinkerMode.Release
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

  def configCliOptions(resourcesExist: Boolean): List[String] =
    gcCliOption() ++
      modeCliOption() ++
      ltoOptions() ++
      clangCliOption() ++
      clangppCliOption() ++
      linkingCliOptions() ++
      compileCliOptions() ++
      resourcesCliOptions(resourcesExist) ++
      targetCliOption()

}

object ScalaNativeOptions {
  implicit val hasHashData: HasHashData[ScalaNativeOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaNativeOptions]     = ConfigMonoid.derive
}
