package scala.build.options

final case class PackageOptions(
  packageTypeOpt: Option[PackageType] = None
)

object PackageOptions {
  implicit val hasHashData: HasHashData[PackageOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[PackageOptions] = ConfigMonoid.derive
}
