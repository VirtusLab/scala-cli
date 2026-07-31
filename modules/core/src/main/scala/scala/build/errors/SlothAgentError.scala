package scala.build.errors

final class SlothAgentError(message: String, cause: Throwable = null)
    extends BuildException(message, cause = cause)
