package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ScopePath

trait RequireDirectiveHandler extends DirectiveHandler {
  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]]
}
