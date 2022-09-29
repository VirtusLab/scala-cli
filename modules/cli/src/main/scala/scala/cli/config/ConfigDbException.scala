package scala.cli.config

import scala.build.errors.BuildException

final class ConfigDbException(cause: Exception)
    extends BuildException("Config DB error", cause = cause)
