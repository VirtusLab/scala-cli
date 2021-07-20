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
}

object InternalDependenciesOptions {
  implicit val hasHashData: HasHashData[InternalDependenciesOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[InternalDependenciesOptions] = ConfigMonoid.derive
}
