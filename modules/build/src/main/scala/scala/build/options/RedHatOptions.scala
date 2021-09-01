package scala.build.options

final case class RedHatOptions(
  license: Option[String] = None,
  release: Option[String] = None,
  architecture: Option[String] = None
)

object RedHatOptions {
  implicit val hasHashData: HasHashData[RedHatOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[RedHatOptions]     = ConfigMonoid.derive
}
