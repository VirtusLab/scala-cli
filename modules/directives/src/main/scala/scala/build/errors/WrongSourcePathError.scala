package scala.build.errors

class WrongSourcePathError(exceptionMessage: String, cause: Throwable)
    extends BuildException(
      message =
        s"""The file path argument in the using directives at could not be found!
           |""".stripMargin + exceptionMessage,
      cause = cause
    )
