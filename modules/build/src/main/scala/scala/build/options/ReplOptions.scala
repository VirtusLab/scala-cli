package scala.build.options

final case class ReplOptions(
  ammoniteVersionOpt: Option[String] = None,
  ammoniteArgs: Seq[String] = Nil
)

object ReplOptions {
  implicit val hasHashData: HasHashData[ReplOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ReplOptions] = ConfigMonoid.derive
}
