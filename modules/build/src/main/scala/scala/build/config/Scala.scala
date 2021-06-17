package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class Scala(
  version: Option[String] = None,
  binaryVersion: Option[String] = None,
  platform: Option[String] = None
)

object Scala {
  implicit val reader: ConfigReader[Scala] = deriveReader
}