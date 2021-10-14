package scala.build.errors

import scala.build.Position

final class DependencyFormatError(
  val dependencyString: String,
  val error: String,
  val originOpt: Option[String] = None,
  positionOpt: Option[Position] = None
) extends BuildException(
      s"Error parsing ${originOpt.getOrElse("")}dependency '$dependencyString': $error",
      positions = positionOpt.toSeq
    )
