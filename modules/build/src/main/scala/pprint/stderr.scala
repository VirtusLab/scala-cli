package pprint

object stderr {

  // Same as pprint.log, except we log things to stderr rather than stdout

  def log[T](
    x: sourcecode.Text[T],
    tag: String = "",
    width: Int = defaultWidth,
    height: Int = defaultHeight,
    indent: Int = defaultIndent
  )(implicit line: sourcecode.Line, fileName: sourcecode.FileName): T = {

    val tagStrs =
      if (tag.isEmpty) Seq()
      else Seq(fansi.Color.Cyan(tag), fansi.Str(" "))

    val prefix = Seq(
      fansi.Color.Magenta(fileName.value),
      fansi.Str(":"),
      fansi.Color.Green(line.value.toString),
      fansi.Str(" "),
      fansi.Color.Cyan(x.source),
      fansi.Str(": ")
    ) ++ tagStrs
    val str = fansi.Str.join(prefix ++ tokenize(x.value, width, height, indent).toSeq: _*)

    System.err.println(str)
    x.value
  }

}
