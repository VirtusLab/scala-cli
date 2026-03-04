package scala.build.errors

import coursier.error.DependencyError

import scala.build.Position

class CoursierDependencyError(val underlying: DependencyError, positions: Seq[Position] = Seq.empty)
    extends BuildException(
      s"Could not fetch dependency: ${underlying.message}",
      positions = positions,
      cause = underlying.getCause
    )
