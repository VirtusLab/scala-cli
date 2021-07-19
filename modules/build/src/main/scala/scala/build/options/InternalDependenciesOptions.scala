package scala.build.options

final case class InternalDependenciesOptions(
       addStubsDependencyOpt: Option[Boolean]            = None,
      addRunnerDependencyOpt: Option[Boolean]            = None,
  addTestRunnerDependencyOpt: Option[Boolean]            = None
) {
  def addStubsDependency: Boolean =
    addStubsDependencyOpt.getOrElse(true)
  def addTestRunnerDependency: Boolean =
    addTestRunnerDependencyOpt.getOrElse(false)

  def orElse(other: InternalDependenciesOptions): InternalDependenciesOptions =
    InternalDependenciesOptions(
           addStubsDependencyOpt = addStubsDependencyOpt.orElse(other.addStubsDependencyOpt),
          addRunnerDependencyOpt = addRunnerDependencyOpt.orElse(other.addRunnerDependencyOpt),
      addTestRunnerDependencyOpt = addTestRunnerDependencyOpt.orElse(other.addTestRunnerDependencyOpt)
    )
}

object InternalDependenciesOptions {
  implicit val hasHashData: HasHashData[InternalDependenciesOptions] = HasHashData.derive
}
