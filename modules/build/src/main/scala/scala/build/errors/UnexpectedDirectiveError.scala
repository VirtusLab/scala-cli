package scala.build.errors

final class UnexpectedDirectiveError(val key: String)
    extends BuildException(s"Unexpected directive: $key}")
