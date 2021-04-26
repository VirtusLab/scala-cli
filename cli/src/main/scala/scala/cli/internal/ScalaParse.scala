package scala.cli.internal

import scalaparse._

import fastparse._, ScalaWhitespace._

object ScalaParse {

  import Scala._

  def Header[_: P]: P[Seq[(Int, Int)]] = {
    def PkgAsEmptyList = P( Pkg ).map(_ => List.empty[(Int, Int)])
    def ImportStartEnd = P( Index ~ Import ~ Index ).map(List(_))
    def TopStat = P( PkgAsEmptyList | ImportStartEnd )
    P( Semis.? ~ TopStat.repX(0, Semis) )
      .map(_.flatten)
  }

    // For some reason Scala doesn't import this by default
  private def `_`[_: P] = scalaparse.Scala.`_`


  def ImportSplitter[_: P]: P[Seq[ammonite.util.ImportTree]] = {
    def IdParser = P( (Id | `_` ).! ).map(
      s => if (s(0) == '`') s.drop(1).dropRight(1) else s
    )
    def Selector = P( IdParser ~ (`=>` ~/ IdParser).? )
    def Selectors = P( "{" ~/ Selector.rep(sep = ","./) ~ "}" )
    def BulkImport = P( `_`).map(
      _ => Seq("_" -> None)
    )
    def Prefix = P( IdParser.rep(1, sep = ".") )
    def Suffix = P( "." ~/ (BulkImport | Selectors) )
    def ImportExpr: P[ammonite.util.ImportTree] = {
      // Manually use `WL0` parser here, instead of relying on WhitespaceApi, as
      // we do not want the whitespace to be consumed even if the WL0 parser parses
      // to the end of the input (which is the default behavior for WhitespaceApi)
      P( Index ~~ Prefix ~~ (WL0 ~~ Suffix).? ~~ Index).map{
        case (start, idSeq, selectors, end) =>
          ammonite.util.ImportTree(idSeq, selectors, start, end)
      }
    }
    P( `import` ~/ ImportExpr.rep(1, sep = ","./) )
  }

}