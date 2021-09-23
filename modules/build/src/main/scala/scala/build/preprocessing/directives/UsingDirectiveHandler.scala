package scala.build.preprocessing.directives

import scala.build.options.BuildOptions

trait UsingDirectiveHandler extends DirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildOptions]]
}
