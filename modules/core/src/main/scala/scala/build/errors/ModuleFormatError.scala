package scala.build.errors

import scala.build.Position

final class ModuleFormatError(
  val moduleString: String,
  val error: String,
  val originOpt: Option[String] = None,
  positions: Seq[Position] = Nil
) extends BuildException(
      s"Error parsing ${originOpt.getOrElse("")}module '$moduleString': $error",
      positions = positions
    )
