package scala.cli.commands

import caseapp._

final case class SharedJavaOptions(
  @Name("J")
    javaOpt: List[String] = Nil,
  javaProp: List[String] = Nil
) {
  def allJavaOpts: Seq[String] =
    javaOpt ++ javaProp.filter(_.nonEmpty).map(_.split("=", 2)).map {
      case Array(k) => s"-D$k"
      case Array(k, v) => s"-D$k=$v"
    }
}
