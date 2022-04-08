package scala.build.errors

class WrongJavaHomePathError(exceptionMessage: String)
    extends BuildException(message =
      s"""The java home path argument in the using directives at could not be found!
         |$exceptionMessage""".stripMargin) {}
//TODO "at where?"
