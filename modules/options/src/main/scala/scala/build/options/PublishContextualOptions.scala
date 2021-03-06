package scala.build.options

import scala.build.options.publish.{ComputeVersion, MaybeConfigPasswordOption, Signer}
import scala.cli.signing.shared.PasswordOption

/** Publishing-related options, that can have different values locally and on CIs */
final case class PublishContextualOptions(
  repository: Option[String] = None,
  repositoryIsIvy2LocalLike: Option[Boolean] = None,
  sourceJar: Option[Boolean] = None,
  docJar: Option[Boolean] = None,
  gpgSignatureId: Option[String] = None,
  gpgOptions: List[String] = Nil,
  signer: Option[Signer] = None,
  secretKey: Option[MaybeConfigPasswordOption] = None,
  secretKeyPassword: Option[MaybeConfigPasswordOption] = None,
  repoUser: Option[PasswordOption] = None,
  repoPassword: Option[PasswordOption] = None,
  repoRealm: Option[String] = None,
  computeVersion: Option[ComputeVersion] = None,
  checksums: Option[Seq[String]] = None
)

object PublishContextualOptions {
  implicit val monoid: ConfigMonoid[PublishContextualOptions] = ConfigMonoid.derive
}
