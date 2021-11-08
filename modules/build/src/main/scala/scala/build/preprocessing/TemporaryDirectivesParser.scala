package scala.build.preprocessing

import fastparse.NoWhitespace.noWhitespaceImplicit
import fastparse._

import scala.build.Position
import scala.build.preprocessing.directives.Directive

object TemporaryDirectivesParser {

  private def ws[_: P]        = P(" ".rep(1))
  private def optWs[_: P]     = P(" ".rep(0))
  private def nl[_: P]        = P(("\r".? ~ "\n").rep(1))
  private def emptyLine[_: P] = P(ws.rep() ~ nl)

  private def singleLineComment[_: P] =
    P(ws.rep() ~ !("//" ~ optWs ~ "require") ~ !("//" ~ optWs ~ "using") ~ "//" ~ P(CharPred(c =>
      c != '\n'
    )).rep() ~ nl)
      .map(_ => ())

  private def directive[_: P] = {
    def sc = P(";")
    def tpe = {
      def commentedUsingTpe = P("//" ~ optWs ~ Index ~ "using")
        .map(actualStartIdx => (Directive.Using: Directive.Type, Some(actualStartIdx)))
      def usingKeywordTpe = P("using")
        .map(_ => (Directive.Using: Directive.Type, None))
      def usingTpe = P(ws.? ~ (commentedUsingTpe | usingKeywordTpe) ~ !(ws ~ "target"))
      def commentedRequireTpe = P("//" ~ optWs ~ Index ~ "require")
        .map(actualStartIdx => (Directive.Require: Directive.Type, Some(actualStartIdx)))
      def requireKeywordTpe = P("require")
        .map(_ => (Directive.Require: Directive.Type, None))
      def commentedUsingTargetTpe = P("//" ~ optWs ~ Index ~ "using" ~ ws ~ "target")
        .map(actualStartIdx => (Directive.Require: Directive.Type, Some(actualStartIdx)))
      def usingTargetKeywordTpe = P("using" ~ ws ~ "target")
        .map(_ => (Directive.Require: Directive.Type, None))
      def requireTpe = P(
        ws.? ~ (commentedRequireTpe | requireKeywordTpe | commentedUsingTargetTpe | usingTargetKeywordTpe)
      )
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
    def elem       = P(!("in" ~ (ws | "," | nl)) ~ (simpleElem | quotedElem))

    def parser = P(
      Index ~ tpe ~ ws ~
        (elem.rep(1, sep = P(ws)) ~ (ws ~ "in" ~ ws ~ elem).?).rep(1, sep = P(ws.? ~ "," ~ ws.?)) ~
        Index ~
        nl.? ~ sc.? ~
        nl.?
    )

    parser.map {
      case (startIdx, (tpe0, actualStartIdxOpt), allElems, endIdx) =>
        val isComment = actualStartIdxOpt.nonEmpty
        allElems.map {
          case (elems, scopeOpt) =>
            val pos = Position.Raw(actualStartIdxOpt.getOrElse(startIdx), endIdx)
            Directive(tpe0, elems, scopeOpt, isComment, pos)
        }
    }
  }

  private def maybeDirective[_: P] =
    // TODO Use some cuts above to also catch malformed directives?
    P((emptyLine | singleLineComment).rep() ~ directive.?)

  private def parseDirective(content: String, fromIndex: Int): Option[(Seq[Directive], Int)] = {
    // TODO Don't create a new String here
    val res = parse(content.drop(fromIndex), maybeDirective(_))
    res.fold(
      (err, _, _) => sys.error(err),
      (dirOpt, idx) => {
        def updatePos(pos: Position): Position =
          pos match {
            case r: Position.Raw => r + fromIndex
            case _               => pos
          }
        dirOpt
          .map { directives =>
            directives.map(dir => dir.copy(position = updatePos(dir.position)))
          }
          .map((_, idx + fromIndex))
      }
    )
  }

  def parseDirectives(
    path: Either[String, os.Path],
    content: String
  ): Option[(List[Directive], Option[String])] = {

    def helper(fromIndex: Int, acc: List[Directive]): (List[Directive], Int) =
      parseDirective(content, fromIndex) match {
        case None                  => (acc.reverse, fromIndex)
        case Some((dir, newIndex)) => helper(newIndex, dir.toList ::: acc)
      }

    val (rawDirectives, codeStartsAt) = helper(0, Nil)
    val f                             = Position.Raw.filePos(path, content)
    def updatePos(pos: Position): Position =
      pos match {
        case r: Position.Raw => f(r)
        case _               => pos
      }
    val directives = rawDirectives.map { dir =>
      dir.copy(position = updatePos(dir.position))
    }

    if (codeStartsAt == 0) {
      assert(directives.isEmpty)
      None
    }
    else {
      val onlyCommentedDirectives = directives.forall(_.isComment)
      val updatedContentOpt =
        if (onlyCommentedDirectives) None
        else
          Some {
            content.take(codeStartsAt).map(c => if (c.isControl) c else ' ') ++
              content.drop(codeStartsAt)
          }
      Some((directives, updatedContentOpt))
    }
  }

}
