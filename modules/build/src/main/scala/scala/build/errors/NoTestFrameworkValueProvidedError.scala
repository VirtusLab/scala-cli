package scala.build.errors

final class NoTestFrameworkValueProvidedError extends BuildException(
      "No test framework value provided to using test-framework directive"
    )
