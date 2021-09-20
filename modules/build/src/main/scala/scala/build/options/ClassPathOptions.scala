package scala.build.options

import dependency._

final case class ClassPathOptions(
  extraRepositories: Seq[String] = Nil,
  extraJars: Seq[os.Path] = Nil,
  extraCompileOnlyJars: Seq[os.Path] = Nil,
  extraSourceJars: Seq[os.Path] = Nil,
  fetchSources: Option[Boolean] = None,
  extraDependencies: Seq[AnyDependency] = Nil
)

object ClassPathOptions {
  implicit val hasHashData: HasHashData[ClassPathOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ClassPathOptions]     = ConfigMonoid.derive
}
