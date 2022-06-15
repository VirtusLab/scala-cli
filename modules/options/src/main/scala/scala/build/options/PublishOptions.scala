package scala.build.options

import scala.build.Positioned
import scala.build.options.publish.{Developer, License, Vcs}

final case class PublishOptions(
  organization: Option[Positioned[String]] = None,
  name: Option[Positioned[String]] = None,
  moduleName: Option[Positioned[String]] = None,
  version: Option[Positioned[String]] = None,
  url: Option[Positioned[String]] = None,
  license: Option[Positioned[License]] = None,
  versionControl: Option[Vcs] = None,
  description: Option[String] = None,
  developers: Seq[Developer] = Nil,
  scalaVersionSuffix: Option[String] = None,
  scalaPlatformSuffix: Option[String] = None,
  contextual: PublishContextualOptions = PublishContextualOptions()
)

object PublishOptions {
  implicit val monoid: ConfigMonoid[PublishOptions] = ConfigMonoid.derive
}
