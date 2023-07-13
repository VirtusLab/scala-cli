package scala.build.errors

import scala.build.Position
import scala.build.errors.Diagnostic.TextEdit

trait Diagnostic {
  def message: String
  def severity: Severity
  def positions: Seq[Position]
  def textEdit: Option[TextEdit] = None
}

object Diagnostic {
  case class TextEdit(title: String, newText: String)
  object Messages {
    val bloopTooOld =
      "JVM that is hosting bloop is older than the requested runtime. Please run command `bloop exit`, and then use `--jvm` flag to restart Bloop"
  }

  private case class ADiagnostic(
    message: String,
    severity: Severity,
    positions: Seq[Position],
    override val textEdit: Option[TextEdit]
  ) extends Diagnostic

  def apply(
    message: String,
    severity: Severity,
    positions: Seq[Position] = Nil,
    textEdit: Option[TextEdit] = None
  ): Diagnostic = ADiagnostic(message, severity, positions, textEdit)
}
