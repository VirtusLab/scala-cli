package scala.build.actionable

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic}
import scala.build.options.BuildOptions

object ActionablePreprocessor {
  val actionableHandlers = Seq[ActionableHandler[_]](
    ActionableDependencyHandler
  )

  def generateActionableDiagnostics(
    options: BuildOptions
  ): Either[BuildException, Seq[Diagnostic]] =
    actionableHandlers
      .map(handler => handler.createActionableDiagnostics(options))
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

}
