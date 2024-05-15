package scala.build.preprocessing

import scala.util.matching.Regex

object SheBang {
  private val sheBangRegex: Regex = s"""(^(#!.*(\\X)?)+(\\X*!#.*)?)""".r

  def isShebangScript(content: String): Boolean = sheBangRegex.unanchored.matches(content)

  /** Returns the shebang section and the content without the shebang section */
  def partitionOnShebangSection(content: String): (String, String) =
    if (content.startsWith("#!")) {
      val regexMatch = sheBangRegex.findFirstMatchIn(content)
      regexMatch match {
        case Some(firstMatch) =>
          (firstMatch.toString(), content.replaceFirst(firstMatch.toString(), ""))
        case None => ("", content)
      }
    }
    else
      ("", content)

  def ignoreSheBangLines(content: String): (String, Boolean) =
    if (content.startsWith("#!")) {
      val regexMatch = sheBangRegex.findFirstMatchIn(content)
      regexMatch match {
        case Some(firstMatch) =>
          content.replace(
            firstMatch.toString(),
            System.lineSeparator() * firstMatch.toString().split(System.lineSeparator()).length
          ) -> true
        case None => (content, false)
      }
    }
    else
      (content, false)

}
