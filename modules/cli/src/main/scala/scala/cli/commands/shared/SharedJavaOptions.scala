package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SharedJavaOptions(
  @Group("Java")
  @HelpMessage("Set Java options, such as `-Xmx1g`")
  @ValueDescription("java-options")
  @Tag(tags.must)
  @Tag(tags.important)
  @Name("J")
    javaOpt: List[String] = Nil,
  @Recurse
    javaProperties: JavaPropOptions = JavaPropOptions(),
) {
  // format: on
  def allJavaOpts: Seq[String] =
    javaOpt ++ javaProperties.javaProp.filter(_.nonEmpty).map(_.split("=", 2)).map {
      case Array(k)    => s"-D$k"
      case Array(k, v) => s"-D$k=$v"
    }
}

object SharedJavaOptions {
  implicit lazy val parser: Parser[SharedJavaOptions] = Parser.derive
  implicit lazy val help: Help[SharedJavaOptions]     = Help.derive
}
