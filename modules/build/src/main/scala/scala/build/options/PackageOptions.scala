package scala.build.options

import scala.build.internal.Constants

final case class PackageOptions(
  standalone: Option[Boolean] = None,
  version: Option[String] = None,
  launcherApp: Option[String] = None,
  maintainer: Option[String] = None,
  description: Option[String] = None,
  packageTypeOpt: Option[PackageType] = None,
  logoPath: Option[os.Path] = None,
  macOSidentifier: Option[String] = None,
  debianOptions: DebianOptions = DebianOptions(),
  windowsOptions: WindowsOptions = WindowsOptions(),
  redHatOptions: RedHatOptions = RedHatOptions(),
  dockerOptions: DockerOptions = DockerOptions()
) {

  def packageVersion: String =
    version
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(Constants.version.stripSuffix("-SNAPSHOT"))

  def isDockerEnabled: Boolean = dockerOptions.isDockerEnabled.getOrElse(false)

  // default behaviour for building docker image is building standalone JARs
  def isStandalone: Boolean = standalone.getOrElse(isDockerEnabled)

}

object PackageOptions {
  implicit val monoid: ConfigMonoid[PackageOptions] = ConfigMonoid.derive
}
