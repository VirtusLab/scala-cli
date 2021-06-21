package scala.build.options

import coursier.cache.FileCache
import coursier.util.Task

final case class InternalOptions(
  keepDiagnostics: Boolean = false,
  cache: Option[FileCache[Task]] = None,
  localRepository: Option[String] = None
) {
  def orElse(other: InternalOptions): InternalOptions =
    InternalOptions(
      keepDiagnostics = keepDiagnostics || other.keepDiagnostics,
      cache = cache.orElse(other.cache),
      localRepository = localRepository.orElse(other.localRepository)
    )
}
