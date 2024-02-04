package scala.build.errors

import scala.build.Position

final class ToolkitVersionError(msg: String, positions: Seq[Position])
    extends BuildException(msg, positions)
