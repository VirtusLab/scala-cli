package scala.build.errors

class WrongJarPathError(exceptionMessage: String)
    extends BuildException(message =
      s"""The jar path argument in the using directives at could not be found!
         |$exceptionMessage""".stripMargin) {}
