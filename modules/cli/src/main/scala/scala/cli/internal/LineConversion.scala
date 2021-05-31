package scala.cli.internal

object LineConversion {

  private def line(content: String, idx: Int): Int = {

    val content0 = content.toCharArray

    val CR = '\r'
    val LF = '\n'

    def isAtEndOfLine(idx: Int) = {
      // don't identify the CR in CR LF as a line break, since LF will do.
      val notCRLF0 =
        content0(idx) != CR ||
          !content0.isDefinedAt(idx + 1) ||
          content0(idx + 1) != LF

      notCRLF0 && (content0(idx) == CR || content0(idx) == LF)
    }

    (0 until idx).count(isAtEndOfLine(_))
  }

  def scalaLineToScLine(
    scCode: String,
    scalaCode: String,
    startOffsetInScala: Int
  ): Int => Option[Int] = {
    val startLineInScala = line(scalaCode, startOffsetInScala)
    val scLineCount = line(scCode, scCode.length)

    lineScala =>
      val lineSc = lineScala - startLineInScala
      if (lineSc >= 0 && lineSc < scLineCount) Some(lineSc)
      else None
  }

}
