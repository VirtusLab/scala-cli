package scala.cli.errors

import scala.build.errors.BuildException

final class WrongSonatypeServerError
    extends BuildException(
      "Make sure you're publishing to the right Sonatype server: legacy 'central' or new 'central-s01'"
    )
