package scala.build.errors

import scala.build.preprocessing.directives.Directive

final class UnusedDirectiveError(directive: Directive) extends BuildException(
      s"Unrecognized directive: ${directive.tpe.name} ${directive.values.mkString(" ")}",
      positions = Seq(directive.position)
    )
