package scala.build.errors

import scala.build.Position

final class RepositoryFormatError(errors: ::[String], positions: Seq[Position])
    extends BuildException(
      s"Error parsing repositories: ${errors.mkString(", ")}",
      positions
    )
