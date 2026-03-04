package scala.build.errors

final class ConfigDbException(cause: Exception)
    extends BuildException(s"Config DB error: ${cause.getMessage}", cause = cause)
