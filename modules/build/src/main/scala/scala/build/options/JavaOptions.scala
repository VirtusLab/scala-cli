package scala.build.options

final case class JavaOptions(
  javaHomeOpt: Option[os.Path] = None,
  jvmIdOpt: Option[String] = None,
  jvmIndexOpt: Option[String] = None,
  jvmIndexOs: Option[String] = None,
  jvmIndexArch: Option[String] = None,
  javaOpts: Seq[String] = Nil
)

object JavaOptions {
  implicit val hasHashData: HasHashData[JavaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[JavaOptions] = ConfigMonoid.derive
}
