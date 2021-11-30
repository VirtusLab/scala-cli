package scala.build.preprocessing

import scala.util.matching.Regex

object SheBang {
  private val sheBangRegex: Regex = s"""(^(#!.*(\\r\\n?|\\n)?)+(\\s*!#.*)?)""".r

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
