package scala.build.options

import scala.build.internal.Constants

final case class ReplOptions(
  useAmmoniteOpt: Option[Boolean] = None,
  ammoniteVersionOpt: Option[String] = None,
  ammoniteArgs: Seq[String] = Nil
) {
  def useAmmonite: Boolean =
    useAmmoniteOpt.getOrElse(false)
  def ammoniteVersion: String =
    ammoniteVersionOpt.getOrElse(Constants.ammoniteVersion)
}

object ReplOptions {
  implicit val hasHashData: HasHashData[ReplOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ReplOptions] = ConfigMonoid.derive
}
