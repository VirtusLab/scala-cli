package scala.build.preprocessing.directives

import scala.build.Positioned
import scala.build.preprocessing.ScopePath

case class ScopedValue[T](
  positioned: Positioned[String],
  maybeScopePath: Option[ScopePath] = None
)
