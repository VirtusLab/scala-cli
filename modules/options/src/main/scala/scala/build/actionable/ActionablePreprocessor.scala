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
      .map(handler => handler.createActionableDiagnostics(options))
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

}
