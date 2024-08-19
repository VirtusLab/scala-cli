package scala.build.options

import scala.build.internal.Constants

/** @param jmhVersion
  *   the version of JMH to be used in the build
  * @param enableJmh
  *   toggle for enabling JMH dependency handling in the build (overrides [[runJmh]] when disabled)
  * @param runJmh
  *   toggle for whether JMH should actually be runnable from this build
  */
final case class JmhOptions(
  jmhVersion: Option[String] = None,
  enableJmh: Option[Boolean] = None,
  runJmh: Option[Boolean] = None
) {
  def finalJmhVersion: Option[String] = jmhVersion.orElse(enableJmh match {
    case Some(true) => Some(Constants.jmhVersion)
    case _          => None
  })

  def canRunJmh: Boolean =
    (for { enabled <- enableJmh; runnable <- runJmh } yield enabled && runnable)
      .getOrElse(false)
}

object JmhOptions {
  implicit val hasHashData: HasHashData[JmhOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JmhOptions]     = ConfigMonoid.derive
}
