package scala.build.preprocessing.directives

import scala.build.preprocessing.Scoped

case class PartiallyProcessedDirectives[T](
  global: T,
  scoped: Seq[Scoped[T]],
  unused: Seq[StrictDirective]
) {
  def andThen[U](f: Seq[StrictDirective] => PartiallyProcessedDirectives[U])
    : PartiallyProcessedDirectives[U] = f(unused)
}
