package scala.cli.errors

import scala.build.errors.BuildException

final class GraalVMNativeImageError(msg: String = "Error building native image with GraalVM")
    extends BuildException(msg)
