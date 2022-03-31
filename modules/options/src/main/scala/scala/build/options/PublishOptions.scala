package scala.build.options

import scala.build.Positioned
import scala.build.options.publish.{Developer, License, Signer, Vcs}
import scala.cli.signing.shared.PasswordOption

final case class PublishOptions(
  organization: Option[Positioned[String]] = None,
  name: Option[Positioned[String]] = None,
  version: Option[Positioned[String]] = None,
  url: Option[Positioned[String]] = None,
  license: Option[Positioned[License]] = None,
  versionControl: Option[Vcs] = None,
  description: Option[String] = None,
  developers: Seq[Developer] = Nil,
  scalaVersionSuffix: Option[String] = None,
  scalaPlatformSuffix: Option[String] = None,
  repository: Option[String] = None,
  sourceJar: Option[Boolean] = None,
  docJar: Option[Boolean] = None,
  gpgSignatureId: Option[String] = None,
  gpgOptions: List[String] = Nil,
  signer: Option[Signer] = None,
  secretKey: Option[os.Path] = None,
  secretKeyPassword: Option[PasswordOption] = None
)

object PublishOptions {
  implicit val monoid: ConfigMonoid[PublishOptions] = ConfigMonoid.derive
}
