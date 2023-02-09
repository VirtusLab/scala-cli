package scala.build.errors

import scala.build.Position
import scala.build.errors.Diagnostic.TextEdit

final class UnsupportedAmmoniteImportError(positions: Seq[Position], usingDirText: String)
    extends BuildException(
      "Ammonite imports using \"$ivy\" and \"$dep\" are no longer supported, switch to 'using dep' directive",
      positions
    ) {
  override def textEdit = Some(TextEdit(usingDirText))
}
