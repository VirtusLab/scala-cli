package scala.build.errors

class ParsingInputsException(exceptionMessage: String, cause: Throwable)
    extends BuildException(
      message = exceptionMessage,
      cause = cause
    )
