package scala.build.actionable
import scala.build.Positioned
import scala.build.options.BuildOptions

trait ActionableHandler[V] {

  def extractValues(options: BuildOptions): Seq[Positioned[V]]
  def createActionableDiagnostic(value: Positioned[V], options: BuildOptions): ActionableDiagnostic
}
