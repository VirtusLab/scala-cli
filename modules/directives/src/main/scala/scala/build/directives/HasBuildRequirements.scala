package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ScopePath

trait HasBuildRequirements {
  def buildRequirements: Either[BuildException, BuildRequirements]
}
