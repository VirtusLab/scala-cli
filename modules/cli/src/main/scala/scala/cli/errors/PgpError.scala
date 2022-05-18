package scala.cli.errors

import scala.build.errors.BuildException

final class PgpError(message: String) extends BuildException(message)
