package scala.build.internal

import coursier.core.Repository

final case class ExternalBinaryParams(
  binaryUrl: String,
  changing: Boolean,
  launcherPrefix: String,
  dependencies: Seq[dependency.Dependency],
  mainClass: String,
  forcedVersions: Seq[(dependency.Module, String)] = Nil,
  extraRepos: Seq[Repository] = Nil
)
