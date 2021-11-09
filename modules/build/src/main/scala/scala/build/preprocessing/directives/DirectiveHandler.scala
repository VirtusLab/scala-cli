package scala.build.preprocessing.directives

trait DirectiveHandler {
  def name: String
  def description: String
  def descriptionMd: String = description
  def usage: String
  def usageMd: String       = s"`$usage`"
  def examples: Seq[String] = Nil

  // Strict / using_directives-based directives
  def keys: Seq[String]
}
