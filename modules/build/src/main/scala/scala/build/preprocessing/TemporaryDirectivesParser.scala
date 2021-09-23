package scala.build.preprocessing

import fastparse._
import fastparse.NoWhitespace.noWhitespaceImplicit

import scala.build.preprocessing.directives.Directive

object TemporaryDirectivesParser {

  private def directive[_: P] = {
    def ws = P(" ".rep(1))
    def sc = P(";")
    def nl = P(("\r".? ~ "\n").rep(1))
    def tpe = {
      def usingTpe = P(ws.? ~ ("using" | "@using" | "// using"))
        .map(_ => (Directive.Using: Directive.Type))
      def requireTpe = P(ws.? ~ ("require" | "@require" | "// require"))
        .map(_ => (Directive.Require: Directive.Type))
      P(usingTpe | requireTpe)
    }

    def simpleElemChar = P(
      CharPred(c => !c.isWhitespace && c != '"' && c != ',') |
        P("\\\\").map(_ => "\\") |
        P("\\\"").map(_ => "\"") |
        P("\\,").map(_ => ",")
    )
    def simpleElem = P(simpleElemChar.rep(1).!)
    def charInQuote = P(
      CharPred(c => c != '"' && c != '\\').! |
        P("\\\\").map(_ => "\\") |
        P("\\\"").map(_ => "\"")
    )
    def quotedElem = P("\"" ~ charInQuote.rep ~ "\"").map(_.mkString)
    def elem       = P(simpleElem | quotedElem)

    def parser = P(
      tpe ~ ws ~
        elem.rep(1, sep = P(ws)).rep(1, sep = P(ws.? ~ "," ~ ws.?)) ~
        nl.? ~ sc.? ~
        nl.?
    )

    parser.map {
      case (tpe0, allElems) =>
        allElems.map(elems => Directive(tpe0, elems))
    }
  }

  private def maybeDirective[_: P] = {
    // TODO Use some cuts above to also catch malformed directives?
    P(directive.?)
  }

  private def parseDirective(content: String, fromIndex: Int): Option[(Seq[Directive], Int)] = {
    // TODO Don't create a new String here
    val res = parse(content.drop(fromIndex), maybeDirective(_))
    res.fold((err, idx, _) => sys.error(err), (dirOpt, idx) => dirOpt.map((_, idx + fromIndex)))
  }

  def parseDirectives(content: String): Option[(List[Directive], String)] = {

    def helper(fromIndex: Int, acc: List[Directive]): (List[Directive], Int) =
      parseDirective(content, fromIndex) match {
        case None                  => (acc.reverse, fromIndex)
        case Some((dir, newIndex)) => helper(newIndex, dir.toList ::: acc)
      }

    val (directives, codeStartsAt) = helper(0, Nil)

    if (codeStartsAt == 0) {
      assert(directives.isEmpty)
      None
    }
    else
      Some((
        directives,
        content.take(codeStartsAt).map(c => if (c.isControl) c else ' ') ++
          content.drop(codeStartsAt)
      ))
  }

}
