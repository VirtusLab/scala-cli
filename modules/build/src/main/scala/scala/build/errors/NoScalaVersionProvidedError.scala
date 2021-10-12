package scala.build.errors

final class NoScalaVersionProvidedError extends BuildException(
      "No Scala version provided to using scala directive"
    )
