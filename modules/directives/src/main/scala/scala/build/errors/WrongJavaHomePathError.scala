package scala.build.errors

class WrongJavaHomePathError(javaHomeValue: String, cause: Throwable)
    extends BuildException(
      message =
        s"""The java home path argument in the using directives at $javaHomeValue could not be found!
           |${cause.getLocalizedMessage}""".stripMargin,
      cause = cause
    )
