package scala.build.errors

final class DirectiveErrors(errors: ::[String]) extends BuildException(
      "Directives errors: " + errors.mkString(", ")
    )
