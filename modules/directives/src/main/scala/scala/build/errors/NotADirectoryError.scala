package scala.build.errors

import scala.build.Position

class NotADirectoryError(path: String, positions: Seq[Position])
    extends BuildException(
      message = s"Expected a directory at '$path'".stripMargin,
      positions = positions
    )
