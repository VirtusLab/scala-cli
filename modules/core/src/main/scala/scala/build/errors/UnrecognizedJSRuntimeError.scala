package scala.build.errors

class UnrecognizedJSRuntimeError(runtime: String, validValues: String)
    extends BuildException(s"Unrecognized JS runtime: '$runtime'. Valid values: $validValues")
