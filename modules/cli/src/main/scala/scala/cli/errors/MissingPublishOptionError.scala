package scala.cli.errors

import scala.build.errors.BuildException

final class MissingPublishOptionError(
  val name: String,
  val optionName: String,
  val directiveName: String
) extends BuildException(
      s"Missing $name for publishing, specify one with $optionName or with a 'using $directiveName' directive"
    )
