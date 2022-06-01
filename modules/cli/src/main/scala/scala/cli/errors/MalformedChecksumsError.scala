package scala.cli.errors

import scala.build.errors.BuildException

final class MalformedChecksumsError(input: Seq[String], errors: ::[String])
    extends BuildException(s"Malformed checksums: ${errors.mkString(", ")}")
