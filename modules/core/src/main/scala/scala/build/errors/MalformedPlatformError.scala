package scala.build.errors

final class MalformedPlatformError(marformedInput: String) extends BuildException(
      s"Unrecognized platform: $marformedInput"
    )
