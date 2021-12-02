package scala.build.options

import scala.build.Positioned

final case class JavaOptions(
  javaHomeOpt: Option[Positioned[os.Path]] = None,
  jvmIdOpt: Option[String] = None,
  jvmIndexOpt: Option[String] = None,
  jvmIndexOs: Option[String] = None,
  jvmIndexArch: Option[String] = None,
  javaOpts: Seq[Positioned[String]] = Nil,
  bloopJvmVersion: Option[Positioned[Int]] = None
)

object JavaOptions {
  implicit val hasHashData: HasHashData[JavaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JavaOptions]     = ConfigMonoid.derive
}
