package scala.build.options

import scala.build.Positioned
import scala.build.errors.{BuildException, MalformedInputError}
import scala.build.internal.Licenses
import scala.cli.signing.shared.PasswordOption

final case class PublishOptions(
  organization: Option[Positioned[String]] = None,
  name: Option[Positioned[String]] = None,
  version: Option[Positioned[String]] = None,
  url: Option[Positioned[String]] = None,
  license: Option[Positioned[PublishOptions.License]] = None,
  versionControl: Option[PublishOptions.Vcs] = None,
  description: Option[String] = None,
  developers: Seq[PublishOptions.Developer] = Nil,
  scalaVersionSuffix: Option[String] = None,
  scalaPlatformSuffix: Option[String] = None,
  repository: Option[String] = None,
  sourceJar: Option[Boolean] = None,
  docJar: Option[Boolean] = None,
  gpgSignatureId: Option[String] = None,
  gpgOptions: List[String] = Nil,
  signer: Option[PublishOptions.Signer] = None,
  secretKey: Option[os.Path] = None,
  secretKeyPassword: Option[PasswordOption] = None
)

object PublishOptions {
  implicit val monoid: ConfigMonoid[PublishOptions] = ConfigMonoid.derive

  final case class License(name: String, url: String)
  final case class Developer(id: String, name: String, url: String, mail: Option[String] = None)
  final case class Vcs(url: String, connection: String, developerConnection: String)

  sealed abstract class Signer extends Product with Serializable
  object Signer {
    case object Gpg          extends Signer
    case object BouncyCastle extends Signer
  }

  def parseSigner(input: Positioned[String]): Either[MalformedInputError, Signer] =
    input.value match {
      case "gpg"                 => Right(Signer.Gpg)
      case "bc" | "bouncycastle" => Right(Signer.BouncyCastle)
      case _ => Left(new MalformedInputError("signer", input.value, "gpg|bc", input.positions))
    }

  def parseLicense(input: Positioned[String]): Either[BuildException, Positioned[License]] =
    input.value.split(":", 2) match {
      case Array(name) =>
        Licenses.map.get(name) match {
          case None =>
            Left(new MalformedInputError(
              "license",
              input.value,
              "license-id|license-id:url",
              input.positions
            ))
          case Some(license) =>
            Right(input.map(_ => License(name, license.url)))
        }
      case Array(name, url) =>
        Right(input.map(_ => License(name, url)))
    }
  def parseDeveloper(input: Positioned[String]): Either[BuildException, Developer] =
    input.value.split("|", 4) match {
      case Array(id, name, url) =>
        Right(Developer(id, name, url))
      case Array(id, name, url, mail) =>
        Right(Developer(id, name, url, Some(mail).map(_.trim).filter(_.nonEmpty)))
      case _ =>
        Left(
          new MalformedInputError("developer", input.value, "id|name|URL", input.positions)
        )
    }
  def parseVcs(input: Positioned[String]): Either[BuildException, Vcs] =
    if (input.value.startsWith("github:"))
      input.value.stripPrefix("github:").split("/", 2) match {
        case Array(org, project) =>
          val vcs = Vcs(
            s"https://github.com/$org/$project.git",
            s"scm:git:github.com/$org/$project.git",
            s"scm:git:git@github.com:$org/$project.git"
          )
          Right(vcs)
        case _ =>
          Left(
            new MalformedInputError(
              "github-vcs",
              input.value,
              "github:org/project",
              input.positions
            )
          )
      }
    else
      input.value.split("|", 3) match {
        case Array(url, conn, devConn) =>
          val vcs = Vcs(url, conn, devConn)
          Right(vcs)
        case _ =>
          Left(
            new MalformedInputError(
              "vcs",
              input.value,
              "url|connection|developer-connection",
              input.positions
            )
          )
      }
}
