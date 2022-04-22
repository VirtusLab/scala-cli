package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.Positioned
import scala.build.preprocessing.ScopePath

case class ScopedValue[T <: Value[_]](
  positioned: Positioned[String],
  maybeScopePath: Option[ScopePath] = None
)
