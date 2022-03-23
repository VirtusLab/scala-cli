package scala.build.options.packaging

import scala.build.options.ConfigMonoid

final case class DebianOptions(
  conflicts: List[String] = Nil,
  dependencies: List[String] = Nil,
  architecture: Option[String] = None
)

object DebianOptions {
  implicit val monoid: ConfigMonoid[DebianOptions] = ConfigMonoid.derive
}
