package scala.build.actionable

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic}
import scala.build.options.BuildOptions

object ActionablePreprocessor {
  val actionableHandlers = Seq[ActionableHandler[_]](
    ActionableDependencyHandler
  )

  def generateActionableDiagnostics(
    options: BuildOptions
  ): Either[BuildException, Seq[ActionableDiagnostic]] =
    actionableHandlers
      .map(handler => handler.createActionableDiagnostics(options))
      .sequence
      .left.map(CompositeBuildException(_))
      .map((v: Seq[Seq[Any]]) => v.flatten)
      .asInstanceOf[Either[BuildException, Seq[ActionableDiagnostic]]]

}
