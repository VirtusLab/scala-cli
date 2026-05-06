package scala.build.errors

final class BunVersionTooOldForWasmError(found: Int)
    extends BuildException(
      s"Scala.js WASM backend requires Bun >= 1, but found Bun $found. " +
        "Upgrade Bun (https://bun.sh/) or switch runtime via --wasm-runtime node|deno."
    )
