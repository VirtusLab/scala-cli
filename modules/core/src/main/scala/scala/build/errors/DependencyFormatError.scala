package scala.build.errors

import scala.build.Position

final class DependencyFormatError(
  val dependencyString: String,
  val error: String,
  positions: Seq[Position]
) extends BuildException(
      s"Error parsing dependency '$dependencyString': $error",
      positions = positions
    )
