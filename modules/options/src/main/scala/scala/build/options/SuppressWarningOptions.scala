package scala.build.options

final case class SuppressWarningOptions(
  suppressDirectivesInMultipleFilesWarning: Option[Boolean] = None
)

object SuppressWarningOptions {
  implicit val hasHashData: HasHashData[SuppressWarningOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[SuppressWarningOptions]     = ConfigMonoid.derive
}
