package scala.build.errors

import scala.build.Position

final class NoScalaVersionProvidedError(
  val dep: dependency.AnyDependency,
  positions: Seq[Position] = Nil
) extends BuildException(
      s"Got Scala dependency ${dep.render}, but no Scala version is provided",
      positions = positions
    )
