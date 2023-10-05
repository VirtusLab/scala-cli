package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectives
import com.virtuslab.using_directives.custom.utils.ast.*

import scala.annotation.tailrec
import scala.build.Position
import scala.jdk.CollectionConverters.*

object UsingDirectivesOps {
  extension (ud: UsingDirectives) {
    def keySet: Set[String] = ud.getFlattenedMap.keySet().asScala.map(_.toString).toSet
    def containsTargetDirectives: Boolean = ud.keySet.exists(_.startsWith("target."))

    def getPosition(path: Either[String, os.Path]): Position.File =
      extension (pos: Positioned) {
        def getLine   = pos.getPosition.getLine
        def getColumn = pos.getPosition.getColumn
      }

      @tailrec
      def getEndPostion(ast: UsingTree): (Int, Int) = ast match {
        case uds: UsingDefs => uds.getUsingDefs.asScala match {
            case _ :+ lastUsingDef => getEndPostion(lastUsingDef)
            case _                 => (uds.getLine, uds.getColumn)
          }
        case ud: UsingDef => getEndPostion(ud.getValue)
        case uvs: UsingValues => uvs.getValues.asScala match {
            case _ :+ lastUsingValue => getEndPostion(lastUsingValue)
            case _                   => (uvs.getLine, uvs.getColumn)
          }
        case sl: StringLiteral => (
            sl.getLine,
            sl.getColumn + sl.getValue.length + { if sl.getIsWrappedDoubleQuotes then 2 else 0 }
          )
        case bl: BooleanLiteral => (bl.getLine, bl.getColumn + bl.getValue.toString.length)
        case el: EmptyLiteral   => (el.getLine, el.getColumn)
      }

      val (line, column) = getEndPostion(ud.getAst)

      Position.File(path, (0, 0), (line, column), ud.getCodeOffset)

    def getDirectives =
      ud.getAst match {
        case usingDefs: UsingDefs =>
          usingDefs.getUsingDefs.asScala.toSeq
        case _ =>
          Nil
      }

    def nonEmpty: Boolean = !isEmpty
    def isEmpty: Boolean  = ud.getFlattenedMap.isEmpty
  }
}
