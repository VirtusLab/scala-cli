package scala.build

import scala.build.internal.Constants
import scala.build.options.BuildOptions

case class CrossBuildParams(scalaVersion: String, platform: String) {
  def asString: String = s"Scala $scalaVersion, $platform"
}

object CrossBuildParams {
  def apply(buildOptions: BuildOptions) = new CrossBuildParams(
    scalaVersion = buildOptions.scalaOptions.scalaVersion
      .map(_.asString)
      .getOrElse(Constants.defaultScalaVersion),
    platform = buildOptions.platform.value.repr
  )
}
