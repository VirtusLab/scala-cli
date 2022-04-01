package scala.build.options

import coursier.cache.FileCache
import coursier.util.Task

import scala.build.Directories

final case class InternalOptions(
  keepDiagnostics: Boolean = false,
  cache: Option[FileCache[Task]] = None,
  directories: Option[Directories] = None,
  localRepository: Option[String] = None,
  verbosity: Option[Int] = None,
  // FIXME Should be removed, not a real option (not meant to be set from using directives)
  strictBloopJsonCheck: Option[Boolean] = None,
  putPlatformConfigInBloopFile: Option[Boolean] = None
) {
  def verbosityOrDefault = verbosity.getOrElse(0)
  def strictBloopJsonCheckOrDefault =
    strictBloopJsonCheck.getOrElse(InternalOptions.defaultStrictBloopJsonCheck)
}

object InternalOptions {

  def defaultStrictBloopJsonCheck = true

  implicit val hasHashData: HasHashData[InternalOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[InternalOptions]     = ConfigMonoid.derive
}
