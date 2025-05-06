package scala.build.actionable

import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, Diagnostic}
import scala.build.options.BuildOptions

trait ActionableHandler[A <: Diagnostic] {

  /** Type of setting used to generate actionable diagnostic
    */
  type Setting

  /** Extract settings on the basis of which actionable diagnostics will be generated
    *
    * @param options
    *   the Build Options to extract settings
    * @return
    *   the list of settings on the basis of which actionable diagnostics will be generated
    */
  def extractSettings(options: BuildOptions): Seq[Setting]

  /** The setting on the basis of which the Actionable Diagnostic is generated
    *
    * @param option
    *   this option is used to generate an actionable diagnostic
    * @param buildOptions
    *   used to extract additional parameter from buildOptions, such as "ScalaParams" or "Coursier
    *   Cache" See [[ActionableDependencyHandler]]
    */
  def actionableDiagnostic(
    setting: Setting,
    buildOptions: BuildOptions,
    loggerOpt: Option[Logger]
  ): Either[BuildException, Option[A]]

  final def createActionableDiagnostics(
    buildOptions: BuildOptions,
    loggerOpt: Option[Logger] = None
  ): Either[BuildException, Seq[A]] =
    extractSettings(buildOptions)
      .map(v => actionableDiagnostic(v, buildOptions, loggerOpt))
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)
}
