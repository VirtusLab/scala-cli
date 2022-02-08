package scala.build.errors

import scala.build.Position

final class MalformedDirectiveError(message: String, positions: Seq[Position])
    extends BuildException(message, positions)
