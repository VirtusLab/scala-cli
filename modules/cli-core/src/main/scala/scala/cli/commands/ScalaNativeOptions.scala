package scala.cli.commands

import bloop.config.{Config => BloopConfig}
import caseapp._
import java.nio.file.Paths

import scala.build.{Build, BuildOptions, Project}
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

  def buildOptions: BuildOptions.ScalaNativeOptions =
    BuildOptions.ScalaNativeOptions(
      native,
      nativeVersion,
      nativeMode,
      nativeGc,
      nativeClang,
      nativeClangpp,
      nativeLinking,
      nativeLinkingDefaults,
      nativeCompile,
      nativeCompileDefaults
    )

}
