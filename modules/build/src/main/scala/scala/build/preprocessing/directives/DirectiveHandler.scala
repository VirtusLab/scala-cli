package scala.build.preprocessing.directives

import scala.build.options.BuildOptions

trait DirectiveHandler {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil
}
