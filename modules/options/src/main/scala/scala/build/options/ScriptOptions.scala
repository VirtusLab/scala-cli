package scala.build.options

final case class ScriptOptions(
  forceObjectWrapper: Option[Boolean] = None
)

object ScriptOptions {
  implicit val hasHashData: HasHashData[ScriptOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScriptOptions]     = ConfigMonoid.derive
}
