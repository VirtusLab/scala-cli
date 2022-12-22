package scala.build.errors

import scala.build.Position

class WrongSourcePathError(path: String, cause: Throwable, positions: Seq[Position])
    extends BuildException(
      message = s"Invalid path argument '$path' in using directives".stripMargin,
      cause = cause,
      positions = positions
    )
