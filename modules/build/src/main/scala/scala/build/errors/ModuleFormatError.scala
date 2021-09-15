package scala.build.errors

final class ModuleFormatError(
  val moduleString: String,
  val error: String,
  val originOpt: Option[String] = None
) extends BuildException(s"Error parsing ${originOpt.getOrElse("")}module '$moduleString': $error")
