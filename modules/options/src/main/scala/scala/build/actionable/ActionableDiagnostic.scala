package scala.build.actionable

import dependency.*

import scala.build.Position
import scala.build.errors.Diagnostic.TextEdit
import scala.build.errors.{Diagnostic, Severity}

object ActionableDiagnostic {

  case class ActionableDependencyUpdateDiagnostic(
    positions: Seq[Position],
    currentVersion: String,
    newVersion: String,
    dependencyModuleName: String,
    suggestion: String
  ) extends Diagnostic {
    override def message: String =
      s"""|"$dependencyModuleName is outdated, update to $newVersion"
          |     $dependencyModuleName $currentVersion -> $suggestion""".stripMargin

    override def textEdit: Option[TextEdit] = Some(TextEdit(message, suggestion))

    override def severity: Severity = Severity.Hint
  }
}
