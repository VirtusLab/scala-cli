package scala.build.actionable
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.BuildOptions

case object ActionablePreprocessor {
  val actionableHandlers = Seq(
    ActionableDependencyHandler
  )

  def generateActionableDiagnostics(
    options: BuildOptions
  ): Either[BuildException, Seq[ActionableDiagnostic]] =
    actionableHandlers
      .map { handler =>
        handler
          .extractOptions(options)
          .map(v => handler.createActionableDiagnostic(v, options))
          .sequence
          .left.map(CompositeBuildException(_))
          .map(_.flatten)
      }
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

}
