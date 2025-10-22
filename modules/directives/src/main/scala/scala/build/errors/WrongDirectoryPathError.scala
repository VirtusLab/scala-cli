package scala.build.errors

class WrongDirectoryPathError(cause: Throwable) extends BuildException(
      message = s"""The directory path argument in the using directives at could not be found!
                   |${cause.getLocalizedMessage}""".stripMargin,
      cause = cause
    )
