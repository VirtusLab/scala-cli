package scala.build.errors

final class AmbiguousPlatformError(passedPlatformTypes: Seq[String]) extends BuildException(
      s"Ambiguous platform: more than one type of platform has been passed: ${passedPlatformTypes.mkString(", ")}."
    )
