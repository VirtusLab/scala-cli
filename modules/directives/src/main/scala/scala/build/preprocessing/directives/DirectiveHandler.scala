package scala.build.preprocessing.directives

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.preprocessing.ScopePath

trait DirectiveHandler[T] {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil

  def keys: Seq[String]

  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedDirective[T]]

}
