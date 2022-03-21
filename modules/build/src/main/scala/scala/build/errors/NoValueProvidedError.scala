package scala.build.errors

final class NoValueProvidedError(val key: String) extends BuildException(
      s"Expected a value for directive $key",
      // TODO - this seems like outdated thing
      positions = Nil // I wish using_directives provided the key position…
    )
