package scala.build.preprocessing.directives

import scala.build.preprocessing.Scoped

final case class ProcessedDirective[T](global: Option[T], scoped: Seq[Scoped[T]])
