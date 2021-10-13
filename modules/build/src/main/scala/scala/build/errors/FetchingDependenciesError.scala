package scala.build.errors

import coursier.error.CoursierError

final class FetchingDependenciesError(underlying: CoursierError)
    extends BuildException(
      s"Error fetching dependencies: ${underlying.getMessage}",
      Nil,
      underlying
    )
