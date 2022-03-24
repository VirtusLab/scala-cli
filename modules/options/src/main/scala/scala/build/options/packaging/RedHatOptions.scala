package scala.build.options.packaging

import scala.build.options.ConfigMonoid

final case class RedHatOptions(
  license: Option[String] = None,
  release: Option[String] = None,
  architecture: Option[String] = None
)

object RedHatOptions {
  implicit val monoid: ConfigMonoid[RedHatOptions] = ConfigMonoid.derive
}
