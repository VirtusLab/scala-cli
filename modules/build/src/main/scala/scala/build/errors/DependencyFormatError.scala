package scala.build.errors

final class DependencyFormatError(
  val dependencyString: String,
  val error: String,
  val originOpt: Option[String] = None
) extends BuildException(
      s"Error parsing ${originOpt.getOrElse("")}dependency '$dependencyString': $error"
    )
