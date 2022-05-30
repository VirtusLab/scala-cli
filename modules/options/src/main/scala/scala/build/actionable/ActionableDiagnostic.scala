package scala.build.actionable

import scala.build.Position
import scala.build.errors.{Diagnostic, Severity}

trait ActionableDiagnostic {

  /** Provide the message of actionable diagnostic
    */
  def message: String

  /** Provide the old content which will be replaced by actionable diagnostic
    */
  def from: String

  /** Provide the new content which will be replaced by actionable diagnostic
    */
  def to: String
  def positions: Seq[Position]

  final def toDiagnostic: Diagnostic = Diagnostic(
    message = s"""|$message
                  |     From: $from
                  |       To: $to""".stripMargin,
    severity = Severity.Warning,
    positions = positions
  )
}

object ActionableDiagnostic {

  private case class AActionableDiagnostic(
    message: String,
    from: String,
    to: String,
    positions: Seq[Position]
  ) extends ActionableDiagnostic

  def apply(
    message: String,
    from: String,
    to: String,
    positions: Seq[Position] = Nil
  ): ActionableDiagnostic = AActionableDiagnostic(message, from, to, positions)
}
