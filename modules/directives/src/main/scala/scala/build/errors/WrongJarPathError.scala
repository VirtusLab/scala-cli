package scala.build.errors

class WrongJarPathError(cause: Throwable) extends BuildException(
      message = s"""The jar path argument in the using directives at could not be found!
                   |${cause.getLocalizedMessage}""".stripMargin,
      cause = cause
    )
