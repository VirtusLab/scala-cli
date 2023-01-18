package scala.build.actionable

import dependency._

import scala.build.Position
import scala.build.errors.Diagnostic.TextEdit
import scala.build.errors.{Diagnostic, Severity}

abstract class ActionableDiagnostic extends Diagnostic {

  /** Provide the new content which will be replaced by actionable diagnostic
    */
  def suggestion: String

  override def severity = Severity.Hint

  override def textEdit: Option[TextEdit] = Some(TextEdit(suggestion))
}

object ActionableDiagnostic {

  case class ActionableDependencyUpdateDiagnostic(
    positions: Seq[Position],
    currentVersion: String,
    newVersion: String,
    dependencyModuleName: String,
    suggestion: String
  ) extends ActionableDiagnostic {
    override def message: String =
      s"""|"$dependencyModuleName is outdated, update to $newVersion"
          |     $dependencyModuleName $currentVersion -> $suggestion""".stripMargin
  }

}
