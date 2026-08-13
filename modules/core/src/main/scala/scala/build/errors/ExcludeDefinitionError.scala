package scala.build.errors

import scala.build.Position

final class ExcludeDefinitionError private (
  message: String,
  positions: Seq[Position]
) extends BuildException(message, positions)

object ExcludeDefinitionError {

  private def renderedPositions(positions: Seq[Position]): String =
    positions.map(_.render()).distinct.mkString(", ")

  def inUnsupportedFile(
    positions: Seq[Position],
    expectedProjectFilePath: os.Path
  ): ExcludeDefinitionError =
    new ExcludeDefinitionError(
      s"""The `//> using exclude` directive can only be declared in the project configuration file ($expectedProjectFilePath) or in a `.sc` script, but it was found in:
         |  ${renderedPositions(positions)}""".stripMargin,
      positions
    )

  def inMultipleFiles(
    positions: Seq[Position],
    expectedProjectFilePath: os.Path
  ): ExcludeDefinitionError =
    new ExcludeDefinitionError(
      s"""The `//> using exclude` directive must be declared in a single source file, but it was found in:
         |  ${renderedPositions(positions)}
         |It can only be declared in the project configuration file ($expectedProjectFilePath) or in a `.sc` script.""".stripMargin,
      positions
    )
}
