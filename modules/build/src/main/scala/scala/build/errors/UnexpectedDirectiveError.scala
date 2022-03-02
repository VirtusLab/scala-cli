package scala.build.errors

import scala.build.preprocessing.directives.StrictDirective

final class UnexpectedDirectiveError(val directive: StrictDirective)
    extends BuildException(s"Unexpected directive: ${directive.key}")
