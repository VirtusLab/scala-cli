package scala.cli.errors

import scala.build.errors.BuildException
import scala.build.input.{Inputs, Virtual}

final class FoundVirtualInputsError(
  val virtualInputs: Seq[Virtual]
) extends BuildException(
      s"Found virtual inputs: ${virtualInputs.map(_.source).mkString(", ")}"
    ) {
  assert(virtualInputs.nonEmpty)
}
