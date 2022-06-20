package scala.build.actionable

import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait ActionableHandler {
  type V

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
  def createActionableDiagnostic(
    option: V,
    buildOptions: BuildOptions
  ): Either[BuildException, Option[ActionableDiagnostic]]
}
