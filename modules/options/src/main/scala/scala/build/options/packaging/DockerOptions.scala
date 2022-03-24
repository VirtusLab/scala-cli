package scala.build.options.packaging

import scala.build.options.ConfigMonoid

final case class DockerOptions(
  from: Option[String] = None,
  imageRegistry: Option[String] = None,
  imageRepository: Option[String] = None,
  imageTag: Option[String] = None,
  isDockerEnabled: Option[Boolean] = None
)

object DockerOptions {

  implicit val monoid: ConfigMonoid[DockerOptions] = ConfigMonoid.derive

}
