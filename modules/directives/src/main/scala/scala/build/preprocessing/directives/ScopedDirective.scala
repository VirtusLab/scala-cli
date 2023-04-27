package scala.build.preprocessing.directives

import scala.build.preprocessing.ScopePath

case class ScopedDirective(
  directive: StrictDirective,
  maybePath: Either[String, os.Path],
  cwd: ScopePath
)
