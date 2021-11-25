package scala.build.errors

import scala.build.Position

class ScalaVersionError(message: String, positions: Seq[Position] = Nil)
    extends BuildException(message, positions = positions)
