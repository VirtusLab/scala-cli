package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements

trait RequireDirectiveHandler extends DirectiveHandler {
  def handle(directive: Directive): Option[Either[BuildException, BuildRequirements]]
}
