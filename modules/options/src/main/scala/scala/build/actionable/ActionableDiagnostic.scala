package scala.build.actionable

import dependency._

import scala.build.Position
import scala.build.errors.{Diagnostic, Severity}

abstract class ActionableDiagnostic {

  /** Provide the message of actionable diagnostic
    */
  def message: String

  /** Provide the new content which will be replaced by actionable diagnostic
    */
  def to: String
  def positions: Seq[Position]

  final def toDiagnostic: Diagnostic = Diagnostic(
    message = s"""|$message
                  |       To: $to""".stripMargin,
    severity = Severity.Warning,
    positions = positions
  )
}

object ActionableDiagnostic {

  case class ActionableDependencyUpdateDiagnostic(
    message: String,
    positions: Seq[Position],
    oldDependency: AnyDependency,
    newVersion: String
  ) extends ActionableDiagnostic {
    override def to: String = oldDependency.copy(version = newVersion).render
  }

}
