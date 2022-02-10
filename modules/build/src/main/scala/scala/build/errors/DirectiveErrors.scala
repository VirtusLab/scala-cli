package scala.build.errors

import scala.build.Position

final class DirectiveErrors(errors: ::[String], positions: Seq[Position]) extends BuildException(
      "Directives errors: " + errors.mkString(", "),
      positions = positions
    )
