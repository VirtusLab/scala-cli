package scala.build.errors

import scala.build.Position

final class SeveralMainClassesFoundError(
  mainClasses: ::[String],
  positions: Seq[Position]
) extends MainClassError(
      s"Found several main classes: ${mainClasses.mkString(", ")}",
      positions = positions
    )
