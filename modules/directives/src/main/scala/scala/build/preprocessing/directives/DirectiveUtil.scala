package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value
import com.virtuslab.using_directives.custom.model.StringValue
import scala.build.preprocessing.ScopePath
import scala.build.{Position, Positioned}

object DirectiveUtil {

  def position(
    v: Value[_],
    path: Either[String, os.Path],
    skipQuotes: Boolean = false
  ): Position.File = {
    val line   = v.getRelatedASTNode.getPosition.getLine
    val column = v.getRelatedASTNode.getPosition.getColumn + (if (skipQuotes) 1 else 0)
    Position.File(path, (line, column), (line, column))
  }

  def positions(maybePath: Either[String, os.Path], vs: Value[_]*) = vs.map { v =>
    val skipQuotes = v.isInstanceOf[StringValue]
    DirectiveUtil.position(v, maybePath, skipQuotes)
  }

  def scope(v: Value[_], cwd: ScopePath): Option[ScopePath] =
    Option(v.getScope).map((p: String) => cwd / os.RelPath(p))

}
