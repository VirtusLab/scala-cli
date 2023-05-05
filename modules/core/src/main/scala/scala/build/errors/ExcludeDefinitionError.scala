package scala.build.errors

import scala.build.Position

final class ExcludeDefinitionError(positions: Seq[Position], expectedProjectFilePath: os.Path)
    extends BuildException(
      s"""Found exclude directives in files:
         |  ${positions.map(_.render()).distinct.mkString(", ")}
         |exclude directive must be defined in project configuration file: $expectedProjectFilePath.""".stripMargin
    )
