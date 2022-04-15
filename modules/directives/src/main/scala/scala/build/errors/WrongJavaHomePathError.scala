package scala.build.errors

class WrongJavaHomePathError(javaHomeValue: String, exceptionMessage: String)
    extends BuildException(message =
      s"""The java home path argument in the using directives at $javaHomeValue could not be found!
         |$exceptionMessage""".stripMargin)
