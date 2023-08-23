package scala.build.preprocessing.directives

import scala.build.preprocessing.Scoped

case class PartiallyProcessedDirectives[T](
  global: T,
  scoped: Seq[Scoped[T]],
  unused: Seq[StrictDirective],
  experimentalUsed: Seq[StrictDirective]
)
