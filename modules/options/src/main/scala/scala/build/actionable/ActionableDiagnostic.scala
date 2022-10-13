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
    msg: String,
    positions: Seq[Position],
    oldDependency: AnyDependency,
    newVersion: String
  ) extends ActionableDiagnostic {
    override def message: String = s"""|$msg
                                       |     ${oldDependency.render} -> $suggestion""".stripMargin
    override def suggestion: String = oldDependency.copy(version = newVersion).render
  }

}
