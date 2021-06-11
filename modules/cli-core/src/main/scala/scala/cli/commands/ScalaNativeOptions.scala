package scala.cli.commands

import bloop.config.{Config => BloopConfig}
import caseapp._
import java.nio.file.Paths

import scala.build.{Build, Project}
import scala.build.internal.Constants
import scala.scalanative.{build => sn}

final case class ScalaNativeOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala Native")
    native: Boolean = false,

  @Group("Scala Native")
    nativeVersion: Option[String] = None,
  @Group("Scala Native")
    nativeMode: Option[String] = None,
  @Group("Scala Native")
    nativeGc: Option[String] = None,

  @Group("Scala Native")
    nativeClang: Option[String] = None,
  @Group("Scala Native")
    nativeClangpp: Option[String] = None,

  @Group("Scala Native")
    nativeLinking: List[String] = Nil,
  @Group("Scala Native")
    nativeLinkingDefaults: Boolean = true,

  @Group("Scala Native")
    nativeCompile: List[String] = Nil,
  @Group("Scala Native")
    nativeCompileDefaults: Boolean = true

) {

  def finalVersion = nativeVersion.map(_.trim).filter(_.nonEmpty).getOrElse(Constants.scalaNativeVersion)

  private def gc: sn.GC =
    nativeGc.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.GC.default
      case Some(other) => sn.GC(other)
    }
  private def mode: sn.Mode =
    nativeMode.map(_.trim).filter(_.nonEmpty) match {
      case Some("default") | None => sn.Discover.mode()
      case Some(other) => sn.Mode(other)
    }

  private def clang = nativeClang.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clang())
  private def clangpp = nativeClangpp.filter(_.nonEmpty).map(Paths.get(_)).getOrElse(sn.Discover.clangpp())
  private def linkingOptions =
    nativeLinking ++ (if (nativeLinkingDefaults) sn.Discover.linkingOptions() else Nil)
  private def compileOptions =
    nativeCompile ++ (if (nativeCompileDefaults) sn.Discover.compileOptions() else Nil)

  def bloopConfig: BloopConfig.NativeConfig =
    BloopConfig.NativeConfig(
           version = finalVersion,
                     // there are more modes than bloop allows, but that setting here shouldn't end up being used anyway
              mode = if (mode.name == "release") BloopConfig.LinkerMode.Release else BloopConfig.LinkerMode.Debug,
                gc = gc.name,
      targetTriple = None,
             clang = clang,
           clangpp = clangpp,
         toolchain = Nil,
           options = BloopConfig.NativeOptions(
               linker = linkingOptions,
             compiler = compileOptions
           ),
         linkStubs = false,
             check = false,
              dump = false,
            output = None
    )
  def buildOptions(scalaVersions: ScalaVersions): Option[Build.ScalaNativeOptions] =
    if (native) Some(Build.scalaNativeOptions(scalaVersions.version, scalaVersions.binaryVersion, bloopConfig))
    else None

  def config: sn.NativeConfig =
    sn.NativeConfig.empty
      .withGC(gc)
      .withMode(mode)
      .withLinkStubs(false)
      .withClang(clang)
      .withClangPP(clangpp)
      .withLinkingOptions(linkingOptions)
      .withCompileOptions(compileOptions)

}
