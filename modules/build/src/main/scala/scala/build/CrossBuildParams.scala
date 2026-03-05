package scala.build

import dependency.ScalaParameters

import scala.build.internal.Constants
import scala.build.options.BuildOptions

case class CrossBuildParams(scalaVersion: String, platform: String) {
  def asString: String = s"Scala $scalaVersion, $platform"
}

object CrossBuildParams {
  def apply(buildOptions: BuildOptions): CrossBuildParams = new CrossBuildParams(
    scalaVersion = buildOptions.scalaOptions.scalaVersion
      .map(_.asString)
      .getOrElse(Constants.defaultScalaVersion),
    platform = buildOptions.platform.value.repr
  )

  def apply(scalaParams: Option[ScalaParameters], buildOptions: BuildOptions): CrossBuildParams =
    new CrossBuildParams(
      scalaVersion = scalaParams.map(_.scalaVersion)
        .orElse(buildOptions.scalaOptions.scalaVersion.map(_.asString))
        .getOrElse(Constants.defaultScalaVersion),
      platform = buildOptions.platform.value.repr
    )
}
