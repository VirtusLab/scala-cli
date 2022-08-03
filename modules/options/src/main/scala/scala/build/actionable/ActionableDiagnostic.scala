package scala.build.actionable

import dependency._

import scala.build.Position
import scala.build.errors.{Diagnostic, Severity}
import scala.build.errors.Diagnostic.RelatedInformation


abstract class ActionableDiagnostic extends Diagnostic {

  /** Provide the message of actionable diagnostic
    */
  def message: String

  /** Provide the new content which will be replaced by actionable diagnostic
    */
  def suggestion: String

  def positions: Seq[Position]

  override def severity = Severity.Hint

  override def relatedInformation: Option[RelatedInformation] = Some(RelatedInformation(suggestion))
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
