package scala.build.options

import scala.build.Positioned

final case class TestOptions(
  frameworks: Seq[Positioned[String]] = Nil,
  testOnly: Option[String] = None
)

object TestOptions {
  implicit val hasHashData: HasHashData[TestOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[TestOptions]     = ConfigMonoid.derive
}
