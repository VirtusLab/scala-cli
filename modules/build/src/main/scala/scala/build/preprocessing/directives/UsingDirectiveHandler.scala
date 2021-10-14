package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

trait UsingDirectiveHandler extends DirectiveHandler {
  // Loose / fastparse-based directives
  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]]
}
