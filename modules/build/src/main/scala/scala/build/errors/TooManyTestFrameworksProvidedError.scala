package scala.build.errors

final class TooManyTestFrameworksProvidedError extends BuildException(
      "using test-framework directive expects a single value"
    )
