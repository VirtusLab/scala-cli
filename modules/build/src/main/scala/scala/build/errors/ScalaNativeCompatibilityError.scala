package scala.build.errors

final class ScalaNativeCompatibilityError
    extends BuildException(
      """Used Scala Native version is incompatible the passed options.
        |Please try one of the following combinations:
        |  Scala Native version >= 0.4.3 for Scala 3.1 (*.sc & *.scala files)
        |  Scala Native version >= 0.4.0 for Scala 2.13 (*.sc & *.scala files)
        |  Scala Native version >= 0.4.0 for Scala 2.12 (*.scala files)
        |Windows is supported since Scala Native 0.4.1.
        |""".stripMargin
    )
