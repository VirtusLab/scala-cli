package scala.build.errors

class UnrecognizedWasmRuntimeError(runtime: String, validValues: String)
    extends BuildException(s"Unrecognized Wasm runtime: '$runtime'. Valid values: $validValues")
