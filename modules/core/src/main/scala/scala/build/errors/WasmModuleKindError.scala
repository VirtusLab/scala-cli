package scala.build.errors

class WasmModuleKindError
    extends BuildException(
      "Wasm output requires ES modules. Pass --js-module-kind es (or add //> using jsModuleKind es)."
    )
