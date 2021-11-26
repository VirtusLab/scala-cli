package scala.build.options

import dependency._

import scala.build.Positioned

final case class ClassPathOptions(
  extraRepositories: Seq[String] = Nil,
  extraClassPath: Seq[os.Path] = Nil,
  extraCompileOnlyJars: Seq[os.Path] = Nil,
  extraSourceJars: Seq[os.Path] = Nil,
  fetchSources: Option[Boolean] = None,
  extraDependencies: Seq[Positioned[AnyDependency]] = Nil,
  resourceVirtualDir: Seq[os.SubPath] = Nil
)

object ClassPathOptions {
  implicit val hasHashData: HasHashData[ClassPathOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ClassPathOptions]     = ConfigMonoid.derive
}
