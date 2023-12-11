package scala.build.warnings

import scala.build.Position
import scala.build.errors.Diagnostic.TextEdit
import scala.build.errors.{BuildException, Diagnostic, Severity}

final case class DeprecatedWarning(
  message: String,
  positions: Seq[Position],
  override val textEdit: Option[TextEdit]
) extends Diagnostic {
  def severity: Severity = Severity.Warning
}
