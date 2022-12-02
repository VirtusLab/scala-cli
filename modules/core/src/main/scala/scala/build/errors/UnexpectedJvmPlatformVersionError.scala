package scala.build.errors

import scala.build.Position

final class UnexpectedJvmPlatformVersionError(
  version: String,
  positions: Seq[Position]
) extends BuildException(
      s"Unexpected version '$version' specified for JVM platform",
      positions = positions
    )
