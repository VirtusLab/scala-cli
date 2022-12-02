package scala.build.preprocessing.directives

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.preprocessing.Scoped

final case class ProcessedDirective[+T](global: Option[T], scoped: Seq[Scoped[T]]) {
  def map[U](f: T => U): ProcessedDirective[U] =
    ProcessedDirective(global.map(f), scoped.map(_.map(f)))
  def mapE[U](f: T => Either[BuildException, U]): Either[BuildException, ProcessedDirective[U]] = {
    val maybeGlobal = global.map(f) match {
      case None           => Right(None)
      case Some(Left(e))  => Left(e)
      case Some(Right(u)) => Right(Some(u))
    }
    val maybeScoped = scoped.map(_.mapE(f)).sequence.left.map(CompositeBuildException(_))
    (maybeGlobal, maybeScoped)
      .traverseN
      .left.map(CompositeBuildException(_))
      .map {
        case (global0, scoped0) =>
          ProcessedDirective(global0, scoped0)
      }
  }
}
