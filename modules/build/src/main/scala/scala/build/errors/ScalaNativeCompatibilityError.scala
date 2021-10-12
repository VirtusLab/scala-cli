package scala.build.errors

final class ScalaNativeCompatibilityError
    extends BuildException(
      """scala-cli: invalid option: '--native' for scripts is supported only for scala 2.13.*
        |Please try one of the following combinations:
        |  scala-cli --native -S 2.13 <...> (for *.sc & *.scala files)
        |  scala-cli --native -S 2.12 <...> (for *.scala files)
        |""".stripMargin
    )
