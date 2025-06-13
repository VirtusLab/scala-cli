package scala.build.preprocessing

import scala.util.matching.Regex

object SheBang {
  private val sheBangRegex: Regex = s"""(?m)\\A((#![^\\r\\n]*\\R){1,2})""".r

  def isShebangScript(content: String): Boolean = content.startsWith("#!")

  inline def NL = System.lineSeparator

  /** Returns the shebang section and the content without the shebang section */
  def partitionOnShebangSection(content: String): (String, String) = {
    if (content.startsWith("#!")) {
      val (header, body) = content.indexOf("\n!#") match {
      case -1 =>
        sheBangRegex.findFirstMatchIn(content) match {
        case None =>
          ("", content) // throw exception? shouldn't be possible
        case Some(firstMatch) =>
          val header = firstMatch.toString()
          (header.trim, content.replace(header, ""))
        }
      case index =>
        content.splitAt(index+3)
      }
      (header, body)
    }
    else
      ("", content)
  }
  
  def ignoreSheBangLines(content: String): (String, Boolean) = {
    if (content.startsWith("#!"))
      val (header, body) = partitionOnShebangSection(content)
      val blankHeader = NL * header.split("\\R", -1).length
      (blankHeader + body, true)
    else
      (content, false)
  }
}

