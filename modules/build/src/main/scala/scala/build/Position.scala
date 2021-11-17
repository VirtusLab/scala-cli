package scala.build

import scala.collection.mutable

sealed abstract class Position {
  def render(): String =
    render(Os.pwd, java.io.File.separator)
  def render(cwd: os.Path): String =
    render(cwd, java.io.File.separator)
  def render(cwd: os.Path, sep: String): String
}

object Position {

  final case class File(
    path: Either[String, os.Path],
    startPos: (Int, Int),
    endPos: (Int, Int)
  ) extends Position {
    def render(cwd: os.Path, sep: String): String = {
      val p = path match {
        case Left(p0) => p0
        case Right(p0) =>
          if (p0.startsWith(cwd)) p0.relativeTo(cwd).segments.mkString(sep)
          else p0.toString
      }
      if (startPos == endPos)
        s"$p:${startPos._1 + 1}:${startPos._2 + 1}"
      else if (startPos._1 == endPos._1)
        s"$p:${startPos._1 + 1}:${startPos._2 + 1}-${endPos._2 + 1}"
      else
        s"$p:${startPos._1 + 1}:${startPos._2 + 1}-${endPos._1 + 1}:${endPos._2 + 1}"
    }
  }

  final case class Raw(startIdx: Int, endIdx: Int) extends Position {
    def render(cwd: os.Path, sep: String): String =
      s"raw $startIdx:$endIdx"
    def +(shift: Int): Raw =
      Raw(startIdx + shift, endIdx + shift)
  }

  object Raw {

    // from https://github.com/com-lihaoyi/Ammonite/blob/76673f7f3eb9d9ae054482635f57a31527d248de/amm/interp/src/main/scala/ammonite/interp/script/PositionOffsetConversion.scala#L7-L69

    private def lineStartIndices(content: String): Array[Int] = {

      val content0 = content.toCharArray

      // adapted from scala/scala SourceFile.scala

      val length = content0.length
      val CR     = '\r'
      val LF     = '\n'

      def charAtIsEOL(idx: Int)(p: Char => Boolean) = {
        // don't identify the CR in CR LF as a line break, since LF will do.
        def notCRLF0 =
          content0(idx) != CR ||
          !content0.isDefinedAt(idx + 1) ||
          content0(idx + 1) != LF

        idx < length && notCRLF0 && p(content0(idx))
      }

      def isAtEndOfLine(idx: Int) = charAtIsEOL(idx) {
        case CR | LF => true
        case _       => false
      }

      val buf = new mutable.ArrayBuffer[Int]
      buf += 0
      for (i <- 0 until content0.length if isAtEndOfLine(i))
        buf += i + 1
      buf.toArray
    }

    private def offsetToPos(content: String): Int => (Int, Int) = {

      val lineStartIndices0 = lineStartIndices(content)

      def offsetToLine(offset: Int): Int = {

        assert(lineStartIndices0.nonEmpty)

        if (offset >= lineStartIndices0.last) lineStartIndices0.length - 1
        else {
          def find(a: Int, b: Int): Int =
            if (a + 1 >= b) a
            else {
              val c   = (a + b) / 2
              val idx = lineStartIndices0(c)
              if (idx == offset) c
              else if (idx < offset) find(c, b)
              else find(a, c)
            }
          find(0, lineStartIndices0.length - 1)
        }
      }

      offset =>
        assert(offset >= 0)
        assert(offset <= content.length)
        val line = offsetToLine(offset)
        (line, offset - lineStartIndices0(line))
    }

    def filePos(path: Either[String, os.Path], content: String): Raw => File = {
      val f = offsetToPos(content)
      raw =>
        val startPos = f(raw.startIdx)
        val endPos   = f(raw.endIdx)
        File(path, startPos, endPos)
    }
  }
  final case class CommandLine() extends Position {
    def render(cwd: os.Path, sep: String): String = "COMMAND_LINE"
  }

  final case class Custom(msg: String) extends Position {
    def render(cwd: os.Path, sep: String): String = msg
  }

}
