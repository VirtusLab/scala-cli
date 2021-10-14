package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

trait DirectiveHandler {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil

  // Strict / using_directives-based directives
  def keys: Seq[String] = Nil
  def handleValues(values: Seq[Any], cwd: ScopePath): Either[BuildException, BuildOptions] =
    if (keys.isEmpty)
      sys.error("Cannot happen")
    else
      throw new NotImplementedError(
        "using_directives-based directives need to override handleValues"
      )
}
