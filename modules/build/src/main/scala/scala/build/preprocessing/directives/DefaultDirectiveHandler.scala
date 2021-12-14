package scala.build.preprocessing.directives
import os.Path

import scala.build.errors.{BuildException, UnusedDirectiveError}
import scala.build.preprocessing.ScopePath

class DefaultDirectiveHandler[T] extends DirectiveHandler[T] {
  override def name: String = ""

  override def description: String = ""

  override def usage: String = ""

  override def keys: Seq[String] = Seq.empty

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedDirective[T]] = {
    val values = DirectiveUtil.stringValues(
      directive.values,
      path,
      cwd
    ) ++ DirectiveUtil.numericValues(directive.values, path, cwd)
    Left(
      new UnusedDirectiveError(directive.key, values.map(_._1), values.map(_._2))
    )
  }
}
