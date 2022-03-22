package scala.build.errors

import scala.build.Position

final class MalformedInputError(
  val inputType: String,
  val input: String,
  val expectedShape: String,
  positions: Seq[Position] = Nil
) extends BuildException(
      {
        val q = "\""
        s"Malformed $inputType $q$input$q, expected $expectedShape"
      },
      positions = positions
    )
