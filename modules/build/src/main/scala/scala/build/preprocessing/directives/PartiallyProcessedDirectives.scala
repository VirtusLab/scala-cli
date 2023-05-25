package scala.build.preprocessing.directives

import scala.cli.directivehandler.{Scoped, StrictDirective}

case class PartiallyProcessedDirectives[T](
  global: T,
  scoped: Seq[Scoped[T]],
  unused: Seq[StrictDirective]
)
