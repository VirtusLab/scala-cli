package scala.build.errors

import scala.build.Position

class NotAFileError(path: String, positions: Seq[Position])
    extends BuildException(
      message = s"Expected a file at '$path'".stripMargin,
      positions = positions
    )
