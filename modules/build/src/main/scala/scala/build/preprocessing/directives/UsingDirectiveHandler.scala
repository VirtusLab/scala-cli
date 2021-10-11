package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait UsingDirectiveHandler extends DirectiveHandler {
  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]]
}
