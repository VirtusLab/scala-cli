package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, HasScope}
import scala.build.preprocessing.ScopePath

trait HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions]
  HasScope
}
