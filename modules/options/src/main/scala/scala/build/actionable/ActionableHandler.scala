package scala.build.actionable

import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.options.BuildOptions

trait ActionableHandler {

  /** Type of option used to generate actionable diagnostic
    */
  type V

  /** Returned type of actionable diagnostics
    */
  type A <: ActionableDiagnostic

  /** Extract options on the basis of which actionable diagnostics will be generated
    *
    * @param options
    *   the Build Options to extract options
    * @return
    *   the list of options on the basis of which actionable diagnostics will be generated
    */
  def extractOptions(options: BuildOptions): Seq[V]

  /** The option on the basis of which the Actionable Diagnostic is generated
    *
    * @param option
    *   this option is used to generate an actionable diagnostic
    * @param buildOptions
    *   used to extract additional parameter from buildOptions, such as "ScalaParams" or "Coursier
    *   Cache" See [[ActionableDependencyHandler]]
    */
  def actionableDiagnostic(
    option: V,
    buildOptions: BuildOptions
  ): Either[BuildException, Option[A]]

  final def createActionableDiagnostics(
    buildOptions: BuildOptions
  ): Either[BuildException, Seq[A]] =
    extractOptions(buildOptions)
      .map(v => actionableDiagnostic(v, buildOptions))
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)
}
