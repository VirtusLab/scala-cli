package scala.cli.errors

import scala.build.errors.BuildException

final class MissingConfigEntryError(key: String)
    extends BuildException(s"Missing config entry $key")
