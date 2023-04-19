package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, WithBuildRequirements}

trait HasBuildOptionsWithRequirements {
  def buildOptionsWithRequirements
    : Either[BuildException, List[WithBuildRequirements[BuildOptions]]]
}
