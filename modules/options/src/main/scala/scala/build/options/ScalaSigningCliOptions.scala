package scala.build.options

final case class ScalaSigningCliOptions(
  javaArgs: Seq[String] = Nil,
  useJvm: Option[Boolean] = None,
  signingCliVersion: Option[String] = None
)

object ScalaSigningCliOptions {
  implicit val hasHashData: HasHashData[ScalaSigningCliOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaSigningCliOptions]     = ConfigMonoid.derive
}
