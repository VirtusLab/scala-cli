package scala.cli.commands

import caseapp.*

// format: off
final case class SharedJavaOptions(
  @Group("Java")
  @HelpMessage("Set Java options, such as `-Xmx1g`")
  @ValueDescription("java-options")
  @Tag(tags.must)
  @Name("J")
    javaOpt: List[String] = Nil,
  @Group("Java")
  @HelpMessage("Set Java properties")
  @ValueDescription("key=value|key")
  @Tag(tags.must)
    javaProp: List[String] = Nil
) {
  // format: on
  def allJavaOpts: Seq[String] =
    javaOpt ++ javaProp.filter(_.nonEmpty).map(_.split("=", 2)).map {
      case Array(k)    => s"-D$k"
      case Array(k, v) => s"-D$k=$v"
    }
}

object SharedJavaOptions {
  lazy val parser: Parser[SharedJavaOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedJavaOptions, parser.D] = parser
  implicit lazy val help: Help[SharedJavaOptions]                      = Help.derive
}
