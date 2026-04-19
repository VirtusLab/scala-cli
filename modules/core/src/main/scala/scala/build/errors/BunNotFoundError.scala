package scala.build.errors

final class BunNotFoundError extends BuildException(
      "Bun was not found on the PATH. Install Bun from https://bun.sh/ or use --wasm-runtime node"
    )