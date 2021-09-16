package scala.build.errors

final class RepositoryFormatError(errors: ::[String]) extends BuildException(
      s"Error parsing repositories: ${errors.mkString(", ")}"
    )
