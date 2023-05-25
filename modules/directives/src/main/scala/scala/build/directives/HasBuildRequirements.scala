package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.cli.directivehandler.ScopePath

trait HasBuildRequirements {
  def buildRequirements: Either[BuildException, BuildRequirements]
}
