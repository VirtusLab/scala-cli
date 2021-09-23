package scala.build.errors

import scala.build.preprocessing.directives.Directive

final class InvalidDirectiveError(directive: Directive, error: String) extends BuildException(
      s"Invalid directive ${directive.tpe.name} ${directive.values.mkString(" ")}: $error"
    )
