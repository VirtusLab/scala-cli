package scala.build.errors

final class DenoNotFoundError extends BuildException(
      "Deno was not found on the PATH. Install Deno from https://deno.land/ or use --wasm-runtime node"
    )
