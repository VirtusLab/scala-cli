package scala.build

import scala.build.options.{BuildOptions, Platform, Scope}

final case class CrossKey(
  optionsKey: BuildOptions.CrossKey,
  scope: Scope
) {
  def scalaVersion: String =
    optionsKey.scalaVersion
  def platform: Platform =
    optionsKey.platform
}
