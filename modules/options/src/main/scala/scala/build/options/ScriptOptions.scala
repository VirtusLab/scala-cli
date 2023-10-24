package scala.build.options

import scala.build.internal.CodeWrapper

final case class ScriptOptions(
  forceObjectWrapper: Option[Boolean] = None,
  forceDelayedInitWrapper: Option[Boolean] = None
)

object ScriptOptions {
  implicit val hasHashData: HasHashData[ScriptOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScriptOptions]     = ConfigMonoid.derive
}
