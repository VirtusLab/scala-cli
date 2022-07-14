package scala.build.internal

final case class ExternalBinaryParams(
  binaryUrl: String,
  changing: Boolean,
  launcherPrefix: String,
  dependencies: Seq[dependency.Dependency],
  mainClass: String,
  forcedVersions: Seq[(dependency.Module, String)] = Nil,
  extraRepos: Seq[String] = Nil
)
