package scala.build.actionable

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.BuildOptions
import scala.build.errors.Diagnostic

object ActionablePreprocessor {
  val actionableHandlers = Seq[ActionableHandler[_]](
    ActionableDependencyHandler
  )

  def generateActionableDiagnostic(
    options: BuildOptions
  ): Either[BuildException, Seq[ActionableDiagnostic]] =
    actionableHandlers
      .map(handler => handler.createActionableDiagnostics(options))
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

  def generateDiagnostics(
    options: BuildOptions
  ): Either[BuildException, Seq[Diagnostic]] =
    generateActionableDiagnostic(options)
      .map(_.map(_.toDiagnostic))

}
