package scala.build.errors

import coursier.error.CoursierError

import scala.build.Position

final class FetchingDependenciesError(
  underlying: CoursierError,
  positions: Seq[Position]
) extends BuildException(
      underlying.getMessage,
      positions,
      underlying
    )
