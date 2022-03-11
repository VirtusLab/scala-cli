package scala.cli.errors

import scala.build.errors.BuildException

final class MalformedOptionError(
  val optionName: String,
  val optionValue: String,
  val expected: String
) extends BuildException(
      {
        val q = "\""
        s"Malformed option $optionName: got $q$optionValue$q, expected $q$expected$q"
      }
    )
