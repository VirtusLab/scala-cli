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
  /* Using HasHashData.nop here (PublishOptions values are not used during compilation) */
  implicit val hasHashData: HasHashData[ReplOptions] = HasHashData.nop
  implicit val monoid: ConfigMonoid[ReplOptions]     = ConfigMonoid.derive
}
