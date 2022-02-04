package scala.build.errors

import scala.build.preprocessing.directives.StrictDirective

final class NoValueProvidedError(
  val directive: StrictDirective
) extends BuildException(
      s"Expected a value for directive ${directive.key}",
      positions = Nil // I wish using_directives provided the key position…
    ) {
  assert(directive.numericalOrStringValuesCount == 0)
}
