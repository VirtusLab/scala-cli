package scala.build.actionable
import scala.build.Positioned
import scala.build.errors.BuildException
import scala.build.options.BuildOptions

trait ActionableHandler[V] {

  /** Extract options on the basis of which actionable diagnostics will be generated
    *
    * @param options
    *   the Build Options to extract positioned options
    * @return
    *   the list of positioned options
    */
  def extractPositionedOptions(options: BuildOptions): Seq[Positioned[V]]

  /** The option on the basis of which the Actionable Diagnostic is generated
    *
    * @param option
    *   this option is used to generate an actionable diagnostic
    * @param buildOptions
    *   used to extract additional parameter from buildOptions, such as "ScalaParams" or "Coursier
    *   Cache" See [[ActionableDependencyHandler]]
    */
  def createActionableDiagnostic(
    option: Positioned[V],
    buildOptions: BuildOptions
  ): Either[BuildException, Option[ActionableDiagnostic]]
}
