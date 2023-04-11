package scala.cli.errors

import scala.build.errors.BuildException

final class WrongSonatypeServerError(legacyChosen: Boolean)
    extends BuildException(
      s"Wrong Sonatype server, try with ${if legacyChosen then "'central-s01'" else "'central'"}",
    )
