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

  def addHashData(update: String => Unit): Unit = {
    for (add <- addStubsDependencyOpt)
      update("addStubsDependency=" + add.toString + "\n")
    for (add <- addRunnerDependencyOpt)
      update("addRunnerDependency=" + add.toString + "\n")
    for (add <- addTestRunnerDependencyOpt)
      update("addTestRunnerDependency=" + add.toString + "\n")
  }
}
