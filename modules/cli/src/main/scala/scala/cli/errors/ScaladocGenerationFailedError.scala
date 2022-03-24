package scala.cli.errors

import scala.build.errors.BuildException

final class ScaladocGenerationFailedError(val retCode: Int)
    extends BuildException(s"Scaladoc generation failed (exit code: $retCode)") {
  assert(retCode != 0)
}
