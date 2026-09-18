package scala.build

import coursier.version.Version

import scala.build.internal.Constants

extension (s: String) def coursierVersion: Version = Version(s)

extension (csv: Version)
  def isScala38OrNewer: Boolean =
    Constants.scala38Versions
      .map(_.coursierVersion)
      .exists(_ <= csv)

  def isScala39OrNewer: Boolean =
    Constants.scala39Versions
      .map(_.coursierVersion)
      .exists(_ <= csv)
