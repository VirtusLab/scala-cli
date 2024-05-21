package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements

trait HasBuildRequirements {
  def buildRequirements: Either[BuildException, BuildRequirements]
}
