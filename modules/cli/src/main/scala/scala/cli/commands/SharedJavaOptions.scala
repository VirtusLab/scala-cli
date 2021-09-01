package scala.cli.commands

import caseapp._

// format: off
final case class SharedJavaOptions(
  @Group("Java")
  @HelpMessage("Set Java options, such as -Xmx1g")
  @ValueDescription("java-options")
  @Name("J")
    javaOpt: List[String] = Nil,
  @Group("Java")
  @HelpMessage("Set Java properties")
  @ValueDescription("key=value|key")
    javaProp: List[String] = Nil
) {
  // format: on
  def allJavaOpts: Seq[String] =
    javaOpt ++ javaProp.filter(_.nonEmpty).map(_.split("=", 2)).map {
      case Array(k)    => s"-D$k"
      case Array(k, v) => s"-D$k=$v"
    }
}
