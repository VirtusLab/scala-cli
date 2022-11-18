package scala.build.options

import coursier.cache.FileCache
import coursier.util.Task

import scala.build.Positioned
import scala.build.errors.BuildException
import scala.build.interactive.Interactive
import scala.build.interactive.Interactive.InteractiveNop

final case class InternalOptions(
  keepDiagnostics: Boolean = false,
  cache: Option[FileCache[Task]] = None,
  localRepository: Option[String] = None,
  verbosity: Option[Int] = None,
  // FIXME Should be removed, not a real option (not meant to be set from using directives)
  strictBloopJsonCheck: Option[Boolean] = None,
  interactive: Option[() => Either[BuildException, Interactive]] = None,
  javaClassNameVersionOpt: Option[String] = None,
  /** Whether to keep the coursier.Resolution instance in [[scala.build.Artifacts]]
    *
    * Resolutions can be quite heavy in memory (I don't recall the exact numbers, but 10s of MB if
    * not more is not unseen when Spark modules are involves for example), so we only keep them when
    * really needed.
    */
  keepResolution: Boolean = false,
  extraSourceFiles: Seq[Positioned[os.Path]] = Nil,
  addStubsDependencyOpt: Option[Boolean] = None
) {
  def verbosityOrDefault: Int = verbosity.getOrElse(0)
  def strictBloopJsonCheckOrDefault: Boolean =
    strictBloopJsonCheck.getOrElse(InternalOptions.defaultStrictBloopJsonCheck)
  def addStubsDependency: Boolean =
    addStubsDependencyOpt.getOrElse(true)
}

object InternalOptions {

  def defaultStrictBloopJsonCheck = true

  implicit val hasHashData: HasHashData[InternalOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[InternalOptions]     = ConfigMonoid.derive
}
