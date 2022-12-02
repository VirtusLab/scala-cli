package scala.build.directives

final case class DirectiveGroupDetails(
  name: String,
  description: String,
  usage: String,
  descriptionMdOpt: Option[String] = None,
  usageMdOpt: Option[String] = None,
  examples: Seq[String] = Nil
) {
  def descriptionMd: String =
    descriptionMdOpt.getOrElse(description)
  def usageMd: String =
    usageMdOpt.getOrElse(s"`$usage`")
}
