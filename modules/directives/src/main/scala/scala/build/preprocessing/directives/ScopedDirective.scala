package scala.build.preprocessing.directives

import scala.build.errors.UnusedDirectiveError
import scala.build.preprocessing.ScopePath

case class ScopedDirective(
  directive: StrictDirective,
  maybePath: Either[String, os.Path],
  cwd: ScopePath
) {
  def unusedDirectiveError: UnusedDirectiveError = {
    val values =
      DirectiveUtil.concatAllValues(this)
    new UnusedDirectiveError(
      directive.key,
      values.map(_.value),
      values.flatMap(_.positions)
    )
  }
}
