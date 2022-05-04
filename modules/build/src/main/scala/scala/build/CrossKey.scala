package scala.build

import scala.build.options.{BuildOptions, MaybeScalaVersion, Platform, Scope}

final case class CrossKey(
  optionsKey: Option[BuildOptions.CrossKey],
  scope: Scope
) {
  def scalaVersion: MaybeScalaVersion =
    optionsKey
      .map(k => MaybeScalaVersion(k.scalaVersion))
      .getOrElse(MaybeScalaVersion.none)
  def platform: Option[Platform] =
    optionsKey.map(_.platform)
}
