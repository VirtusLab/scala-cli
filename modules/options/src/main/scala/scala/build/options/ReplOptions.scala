package scala.build.options

import scala.build.Logger
import scala.build.internal.Constants

final case class ReplOptions(
  useAmmoniteOpt: Option[Boolean] = None,
  ammoniteVersionOpt: Option[String] = None,
  ammoniteArgs: Seq[String] = Nil
) {
  def useAmmonite: Boolean =
    useAmmoniteOpt.getOrElse(false)
  def ammoniteVersion(scalaVersion: String, logger: Logger): String =
    ammoniteVersionOpt.getOrElse {
      if scalaVersion.startsWith("3.3") then {
        val ammoniteVersionForLts = Constants.ammoniteVersionForScala3Lts
        logger.debug(s"Using the default Ammonite version for Scala 3 LTS: $ammoniteVersionForLts")
        ammoniteVersionForLts
      }
      else Constants.ammoniteVersion
    }
}

object ReplOptions {
  implicit val monoid: ConfigMonoid[ReplOptions] = ConfigMonoid.derive
}
