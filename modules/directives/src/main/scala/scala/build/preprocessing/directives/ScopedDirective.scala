package scala.build.preprocessing.directives

import scala.build.Position
import scala.build.errors.UnusedDirectiveError
import scala.build.preprocessing.ScopePath

case class ScopedDirective(
  directive: StrictDirective,
  maybePath: Either[String, os.Path],
  cwd: ScopePath
) {
  def unusedDirectiveError: UnusedDirectiveError = {
    val values = DirectiveUtil.concatAllValues(this)
    val keyPos = Position.File(
      maybePath,
      (directive.startLine, directive.startColumn),
      (directive.startLine, directive.startColumn + directive.key.length())
    )
    new UnusedDirectiveError(
      directive.key,
      values,
      keyPos
    )
  }
}
