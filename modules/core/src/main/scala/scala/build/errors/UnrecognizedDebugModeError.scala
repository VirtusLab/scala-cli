package scala.build.errors

final class UnrecognizedDebugModeError(mode: String) extends BuildException(
      s"Unrecognized debug mode: $mode."
    )
