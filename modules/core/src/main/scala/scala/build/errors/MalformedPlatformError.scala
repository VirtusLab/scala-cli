package scala.build.errors

import scala.build.Position

final class MalformedPlatformError(
  marformedInput: String,
  positions: Seq[Position] = Nil
) extends BuildException(
      s"Unrecognized platform: $marformedInput",
      positions = positions
    )
