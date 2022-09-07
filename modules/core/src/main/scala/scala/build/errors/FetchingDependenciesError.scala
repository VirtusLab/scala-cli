package scala.build.errors

import coursier.error.CoursierError

import scala.build.Position

final class FetchingDependenciesError(
  val underlying: CoursierError,
  override val positions: Seq[Position]
) extends BuildException(
      underlying.getMessage,
      positions,
      underlying
    )

object FetchingDependenciesError {
  def unapply(e: FetchingDependenciesError): Option[(CoursierError, Seq[Position])] =
    Some(e.underlying -> e.positions)
}
