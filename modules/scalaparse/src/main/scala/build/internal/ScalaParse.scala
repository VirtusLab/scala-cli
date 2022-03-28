package scala.build.internal

import fastparse.ScalaWhitespace._
import fastparse._
import scalaparse._

object ScalaParse {

  import Scala._

  // from https://github.com/com-lihaoyi/Ammonite/blob/0f0d597f04e62e86cbf76d3bd16deb6965331470/amm/compiler/src/main/scala/ammonite/compiler/Parsers.scala#L162-L176
  def formatFastparseError(fileName: String, rawCode: String, f: Parsed.Failure) = {

    val newLine      = System.lineSeparator()
    val lineColIndex = f.extra.input.prettyIndex(f.index)
    val expected     = f.trace().failure.label
    val locationString = {
      val (first, last) = rawCode.splitAt(f.index)
      val lastSnippet   = last.split(newLine).headOption.getOrElse("")
      val firstSnippet = first.reverse
        .split(newLine.reverse)
        .lift(0).getOrElse("").reverse
      firstSnippet + lastSnippet + newLine + (" " * firstSnippet.length) + "^"
    }
    s"$fileName:$lineColIndex expected $expected$newLine$locationString"
  }

  def Header[X: P]: P[Seq[(Int, Int)]] = {
    def PkgAsEmptyList = P(TopPkgSeq).map(_ => List.empty[(Int, Int)])
    def ImportStartEnd = P(Index ~ Import ~ Index).map(List(_))
    def TopStat        = P(PkgAsEmptyList | ImportStartEnd)
    P(Semis.? ~ TopStat.repX(0, Semis))
      .map(_.flatten)
  }

  // For some reason Scala doesn't import this by default
  private def `_`[X: P] = scalaparse.Scala.`_`

  def ImportSplitter[X: P]: P[Seq[ImportTree]] = {
    def IdParser   = P((Id | `_`).!).map(s => if (s(0) == '`') s.drop(1).dropRight(1) else s)
    def Selector   = P(IdParser ~ (`=>` ~/ IdParser).?)
    def Selectors  = P("{" ~/ Selector.rep(sep = ","./) ~ "}")
    def BulkImport = P(`_`).map(_ => Seq("_" -> None))
    def Prefix     = P(IdParser.rep(1, sep = "."))
    def Suffix     = P("." ~/ (BulkImport | Selectors))
    def ImportExpr: P[ImportTree] =
      // Manually use `WL0` parser here, instead of relying on WhitespaceApi, as
      // we do not want the whitespace to be consumed even if the WL0 parser parses
      // to the end of the input (which is the default behavior for WhitespaceApi)
      P(Index ~~ Prefix ~~ (WL0 ~~ Suffix).? ~~ Index).map {
        case (start, idSeq, selectors, end) =>
          ImportTree(idSeq, selectors, start, end)
      }
    P(`import` ~/ ImportExpr.rep(1, sep = ","./))
  }

}
