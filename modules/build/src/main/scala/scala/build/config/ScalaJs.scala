package scala.build.config

import pureconfig.ConfigReader

import scala.build.config.reader.DerivedConfigReader

final case class ScalaJs(
  version: Option[String] = None,
  suffix: Option[String] = None
)

object ScalaJs {
  implicit val reader = DerivedConfigReader[ScalaJs]
}
