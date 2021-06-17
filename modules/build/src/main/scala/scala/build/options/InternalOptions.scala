package scala.build.options

import coursier.cache.FileCache
import coursier.util.Task

final case class InternalOptions(
  keepDiagnostics: Boolean = false,
  cache: Option[FileCache[Task]] = None
) {
  def orElse(other: InternalOptions): InternalOptions =
    InternalOptions(
      keepDiagnostics = keepDiagnostics || other.keepDiagnostics,
      cache = cache.orElse(other.cache)
    )
}
