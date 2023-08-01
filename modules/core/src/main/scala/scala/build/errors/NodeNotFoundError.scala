package scala.build.errors

final class NodeNotFoundError extends BuildException(
      "The Node was not found on the PATH. Please ensure that Node is installed correctly and then try again"
    )
