package scala.build.errors

import scala.build.Position

abstract class MainClassError(
  message: String,
  positions: Seq[Position]
) extends BuildException(message, positions = positions)
