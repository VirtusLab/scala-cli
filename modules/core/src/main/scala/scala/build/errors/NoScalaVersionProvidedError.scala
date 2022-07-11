package scala.build.errors

import scala.build.Position

final class NoScalaVersionProvidedError(
  val depOrModule: Either[dependency.AnyModule, dependency.AnyDependency],
  positions: Seq[Position] = Nil
) extends BuildException(
      {
        val str = depOrModule match {
          case Left(mod)  => "module " + mod.render
          case Right(dep) => "dependency " + dep.render
        }
        s"Got Scala $str, but no Scala version is provided"
      },
      positions = positions
    )
