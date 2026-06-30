package scala.build.errors

final class LazyValGradeError(message: String, cause: Throwable = null)
    extends BuildException(message, cause = cause)
