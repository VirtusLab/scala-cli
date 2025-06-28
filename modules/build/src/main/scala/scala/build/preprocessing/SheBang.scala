package scala.build.preprocessing

object SheBang {
  def isShebangScript(content: String): Boolean = content.startsWith("#!")

  def partitionOnShebangSection(content: String): (String, String, String) =
    if (content.startsWith("#!")) {
      val splitIndex = content.indexOf("\n!#") match {
        case -1 =>
          val eolIndex = content.indexOf("\n")
          content.drop(eolIndex + 1) match {
            case s if s.startsWith("#!") =>
              eolIndex + s.indexOf("\n") + 1 // skip over #! nix-shell line
            case _ =>
              eolIndex + 1
          }
        case index =>
          var i = index + 1
          while (i < content.length && content(i) != '\n') i += 1
          i + 1 // split at start of subsequent line
      }
      val newLine: String = content.drop(splitIndex - 2).take(2) match {
        case CRLF => CRLF
        case _    => "\n"
      }
      val (header, body) = content.splitAt(splitIndex)
      (header, body, newLine)
    }
    else
      ("", content, lineSeparator(content))

  def ignoreSheBangLines(content: String): (String, Boolean, String) =
    if (content.startsWith("#!"))
      val (header, body, newLine) = partitionOnShebangSection(content)
      val blankHeader             = newLine * (header.split("\\R", -1).length - 1)
      (s"$blankHeader$body", true, newLine)
    else
      (content, false, lineSeparator(content))

  def lineSeparator(content: String): String = content.indexOf(CRLF) match {
    case -1 => "\n"
    case _  => CRLF
  }

  private final val CRLF: String = "\r\n"

}
