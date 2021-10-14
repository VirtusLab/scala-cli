package scala.cli.errors

import scala.build.Inputs
import scala.build.errors.BuildException

final class FoundVirtualInputsError(
  val virtualInputs: Seq[Inputs.Virtual]
) extends BuildException(
      s"Found virtual inputs: ${virtualInputs.map(_.source)}"
    ) {
  assert(virtualInputs.nonEmpty)
}
