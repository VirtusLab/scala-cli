package scala.build.options.packaging

import scala.build.Ops.*
import scala.build.Positioned
import scala.build.errors.{BuildException, CompositeBuildException, MalformedInputError}
import scala.build.options.ConfigMonoid

final case class DockerOptions(
  from: Option[String] = None,
  imageRegistry: Option[String] = None,
  imageRepository: Option[String] = None,
  imageTag: Option[String] = None,
  cmd: Option[String] = None,
  isDockerEnabled: Option[Boolean] = None,
  extraDirectories: Seq[os.Path] = Nil,
  exposedPorts: Seq[String] = Nil
)

object DockerOptions {

  implicit val monoid: ConfigMonoid[DockerOptions] = ConfigMonoid.derive

  // same shape as accepted by Jib's Ports.parse, with port numbers restricted to valid ones
  private val exposedPortRegex = """(\d{1,5})(?:-(\d{1,5}))?(?:/(?:tcp|udp))?""".r

  private def isValidPort(port: Int): Boolean = port >= 1 && port <= 65535

  private def validateExposedPort(port: Positioned[String]): Either[BuildException, String] =
    val isValid = port.value match
      case exposedPortRegex(startStr, endStrOrNull) =>
        val start = startStr.toInt
        val end   = Option(endStrOrNull).fold(start)(_.toInt)
        isValidPort(start) && isValidPort(end) && start <= end
      case _ => false
    if isValid then Right(port.value)
    else
      Left(new MalformedInputError(
        "docker-exposed-port",
        port.value,
        "a port (8080) or a port range (9000-9010) between 1 and 65535, optionally followed by a protocol (/tcp or /udp)",
        positions = port.positions
      ))

  def validateExposedPorts(ports: Seq[Positioned[String]]): Either[BuildException, Seq[String]] =
    ports
      .map(validateExposedPort)
      .sequence
      .left.map(CompositeBuildException(_))

}
