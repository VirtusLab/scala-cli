package scala.cli.errors

import scala.build.errors.BuildException

final class FailedToSignFileError(val path: Either[String, os.Path], val error: String)
    extends BuildException(s"Failed to sign ${path.fold(identity, _.toString)}: $error")
