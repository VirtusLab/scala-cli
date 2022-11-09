package scala.cli.errors

import scala.build.errors.BuildException
import scala.build.input.Inputs

final class FoundVirtualInputsError(
  val virtualInputs: Seq[Inputs.Virtual]
) extends BuildException(
      s"Found virtual inputs: ${virtualInputs.map(_.source).mkString(", ")}"
    ) {
  assert(virtualInputs.nonEmpty)
}
