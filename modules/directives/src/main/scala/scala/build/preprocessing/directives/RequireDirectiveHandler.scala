package scala.build.preprocessing.directives

import scala.build.options.BuildRequirements

trait RequireDirectiveHandler extends DirectiveHandler[BuildRequirements] {
  type ProcessedRequireDirective = ProcessedDirective[BuildRequirements]

  override def isRestricted = true
}
