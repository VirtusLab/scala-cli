package scala.build.errors

final class NoDocBuildError extends BuildException(
      "Doc build not present. It may have been cancelled."
    )
