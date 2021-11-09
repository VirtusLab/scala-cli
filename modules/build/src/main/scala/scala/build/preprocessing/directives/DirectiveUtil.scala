package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{StringValue, Value}

import scala.build.Position

object DirectiveUtil {
  def stringValues(values: Seq[Value[_]]): Seq[(String, Position)] =
    values
      .collect {
        case v: StringValue =>
          val offset = v.getRelatedASTNode.getPosition.getOffset
          Seq((v.get, Position.Raw(offset, offset)))
      }
      .flatten
}
