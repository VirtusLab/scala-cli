package scala.build.options

import dependency._

final case class ClassPathOptions(
  extraRepositories: Seq[String] = Nil,
  extraJars: Seq[os.Path] = Nil,
  fetchSources: Option[Boolean] = None,
  extraDependencies: Seq[AnyDependency] = Nil
) {
  def orElse(other: ClassPathOptions): ClassPathOptions =
    ClassPathOptions(
      extraRepositories = extraRepositories ++ other.extraRepositories,
      extraJars = extraJars ++ other.extraJars,
      fetchSources = fetchSources.orElse(other.fetchSources),
      extraDependencies = extraDependencies ++ other.extraDependencies
    )

  def addHashData(update: String => Unit): Unit = {
    for (repo <- extraRepositories)
      update("repositories+=" + repo + "\n")
    for (jar <- extraJars)
      update("jars+=" + jar.toString + "\n")
    for (dep <- extraDependencies)
      update("dependencies+=" + dep.render + "\n")
  }
}
