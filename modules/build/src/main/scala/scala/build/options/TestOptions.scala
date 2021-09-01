package scala.build.options

final case class TestOptions(
  frameworkOpt: Option[String] = None
)

object TestOptions {
  implicit val hasHashData: HasHashData[TestOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[TestOptions]     = ConfigMonoid.derive
}
