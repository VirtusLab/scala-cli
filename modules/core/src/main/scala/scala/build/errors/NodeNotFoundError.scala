package scala.build.errors

import scala.build.Position

final class NodeNotFoundError(positions: Seq[Position] = Nil)
  extends BuildException("NODE not found in path", positions = positions)
