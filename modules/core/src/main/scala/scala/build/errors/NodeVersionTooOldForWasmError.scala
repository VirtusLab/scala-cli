package scala.build.errors

final class NodeVersionTooOldForWasmError(found: Int)
    extends BuildException(
      s"Scala.js Wasm backend requires Node.js >= 22, but found Node.js $found. " +
        "Upgrade Node (https://nodejs.org/) or switch runtime via --js-wasm-runtime deno|bun."
    )
