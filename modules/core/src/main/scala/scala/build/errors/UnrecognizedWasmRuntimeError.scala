package scala.build.errors

class UnrecognizedWasmRuntimeError(runtime: String, validValues: String)
    extends BuildException(s"Unrecognized WASM runtime: '$runtime'. Valid values: $validValues")
