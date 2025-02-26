package scala.build.errors

import scala.build.Position

final class UnusedDirectiveError(key: String, values: Seq[String], position: Position)
    extends BuildException(
      s"Unrecognized directive: $key${
          if values.isEmpty then "" else s" with values: ${values.mkString(", ")}"
        }",
      positions = List(position)
    )
