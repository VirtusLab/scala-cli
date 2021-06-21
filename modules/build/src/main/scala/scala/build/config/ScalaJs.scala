package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class ScalaJs(
  version: Option[String] = None,
  suffix: Option[String] = None
)

object ScalaJs {
  implicit val reader: ConfigReader[ScalaJs] = deriveReader
}
