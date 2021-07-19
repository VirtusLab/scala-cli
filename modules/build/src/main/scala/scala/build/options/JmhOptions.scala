package scala.build.options

final case class JmhOptions(
  addJmhDependencies: Option[String] = None,
  runJmh: Option[Boolean] = None
)

object JmhOptions {
  implicit val hasHashData: HasHashData[JmhOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JmhOptions] = ConfigMonoid.derive
}
