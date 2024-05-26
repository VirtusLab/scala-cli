package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions]
}
