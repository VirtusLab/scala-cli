package scala.build.errors

final class BuildInfoGenerationError(cause: Exception)
    extends BuildException(s"BuildInfo generation error: ${cause.getMessage}", cause = cause)
