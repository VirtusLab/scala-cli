package scala.build.options

final case class InternalDependenciesOptions(
  addTestRunnerDependencyOpt: Option[Boolean] = None
) {
  def addTestRunnerDependency: Boolean =
    addTestRunnerDependencyOpt.getOrElse(false)
}

object InternalDependenciesOptions {
  implicit val hasHashData: HasHashData[InternalDependenciesOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[InternalDependenciesOptions]     = ConfigMonoid.derive
}
