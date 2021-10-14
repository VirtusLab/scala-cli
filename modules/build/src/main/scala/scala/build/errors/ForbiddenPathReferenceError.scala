package scala.build.errors

import scala.build.Position

final class ForbiddenPathReferenceError(
  val virtualRoot: String,
  val positionOpt: Option[Position]
) extends BuildException(
      s"Can't reference paths from sources from $virtualRoot",
      positionOpt.toSeq
    )
