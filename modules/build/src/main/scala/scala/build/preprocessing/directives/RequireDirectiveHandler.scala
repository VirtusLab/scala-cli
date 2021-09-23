package scala.build.preprocessing.directives

import scala.build.options.BuildRequirements

trait RequireDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildRequirements]]
}
