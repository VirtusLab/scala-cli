package scala.build.options

final case class DockerOptions(
  from: Option[String] = None,
  imageRegistry: Option[String] = None,
  imageRepository: Option[String] = None,
  imageTag: Option[String] = None,
  isDockerEnabled: Option[Boolean] = None
)

object DockerOptions {

  implicit val hasHashData: HasHashData[DockerOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[DockerOptions]     = ConfigMonoid.derive

}
