package scala.build.preprocessing.directives

import scala.build.options.BuildOptions

trait UsingDirectiveHandler extends DirectiveHandler[BuildOptions] {
  type ProcessedUsingDirective = ProcessedDirective[BuildOptions]
}
