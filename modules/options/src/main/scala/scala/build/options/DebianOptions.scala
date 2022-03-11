package scala.build.options

final case class DebianOptions(
  conflicts: List[String] = Nil,
  dependencies: List[String] = Nil,
  architecture: Option[String] = None
)

object DebianOptions {
  implicit val monoid: ConfigMonoid[DebianOptions] = ConfigMonoid.derive
}
