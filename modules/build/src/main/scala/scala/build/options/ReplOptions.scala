package scala.build.options

final case class ReplOptions(
  ammoniteVersionOpt: Option[String] = None
)

object ReplOptions {
  implicit val hasHashData: HasHashData[ReplOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ReplOptions] = ConfigMonoid.derive
}
