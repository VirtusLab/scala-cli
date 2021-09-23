package scala.build.errors

import scala.build.preprocessing.directives.Directive

final class UnusedDirectiveError(directive: Directive) extends BuildException(
      s"Unused directive: ${directive.tpe.name} ${directive.values.mkString(" ")}"
    )
