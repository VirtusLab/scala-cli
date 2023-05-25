package scala.build.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, HasScope}
import scala.cli.directivehandler.ScopePath

trait HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions]
  HasScope
}
