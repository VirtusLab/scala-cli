package scala.build.options

final case class ReplOptions(
  useJshellOpt: Option[Boolean] = None
) {
  def useJshell: Boolean =
    useJshellOpt.getOrElse(false)
}

object ReplOptions {
  implicit val monoid: ConfigMonoid[ReplOptions] = ConfigMonoid.derive
}
