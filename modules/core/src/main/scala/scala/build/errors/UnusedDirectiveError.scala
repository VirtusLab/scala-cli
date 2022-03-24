package scala.build.errors

import scala.build.Position

final class UnusedDirectiveError(key: String, values: Seq[String], positions: Seq[Position])
    extends BuildException(
      s"Unrecognized directive: $key with values: ${values.mkString(", ")}",
      positions = positions
    )
