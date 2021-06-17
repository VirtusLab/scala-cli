package scala.build.options

final case class ClassPathOptions(
  extraRepositories: Seq[String] = Nil,
  extraJars: Seq[os.Path] = Nil,
  fetchSources: Option[Boolean] = None
) {
  def orElse(other: ClassPathOptions): ClassPathOptions =
    ClassPathOptions(
      extraRepositories = extraRepositories ++ other.extraRepositories,
      extraJars = extraJars ++ other.extraJars,
      fetchSources = fetchSources.orElse(other.fetchSources)
    )

  def addHashData(update: String => Unit): Unit = {

    for (jar <- extraJars)
      update("jars+=" + jar.toString + "\n")

  }
}
