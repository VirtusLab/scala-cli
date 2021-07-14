package scala.build.config

import pureconfig.ConfigReader

import scala.build.config.reader.DerivedConfigReader

final case class Scala(
  version: Option[String] = None,
  binaryVersion: Option[String] = None,
  platform: Option[String] = None,
  options: List[String] = Nil
)

object Scala {
  implicit val reader = DerivedConfigReader[Scala]
}
