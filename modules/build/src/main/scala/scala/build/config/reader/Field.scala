package scala.build.config.reader

import pureconfig.ConfigReader

final case class Field(
  prefix: List[String],
  name: String,
  rawTypeDescription: String,
  description: Option[String],
  reader: () => ConfigReader[_]
) {
  def mdTypeDescription: String = {
    var d = rawTypeDescription
    if (d.startsWith("Option[") && d.endsWith("]"))
      d = d.stripPrefix("Option[").stripSuffix("]")
    if (d.startsWith("List[") && d.endsWith("]"))
      "list of `" + d.stripPrefix("List[").stripSuffix("]") + "`"
    else
      s"`$d`"
  }
}
