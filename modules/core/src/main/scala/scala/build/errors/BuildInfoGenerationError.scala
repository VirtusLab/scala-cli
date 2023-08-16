package scala.build.errors

import scala.build.Position

final class BuildInfoGenerationError(msg: String, positions: Seq[Position], cause: Exception)
    extends BuildException(
      s"BuildInfo generation error: $msg",
      positions = positions,
      cause = cause
    )
