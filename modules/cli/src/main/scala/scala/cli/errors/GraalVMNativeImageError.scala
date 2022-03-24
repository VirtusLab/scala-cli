package scala.cli.errors

import scala.build.errors.BuildException

final class GraalVMNativeImageError()
    extends BuildException(s"Error building native image with GraalVM")
