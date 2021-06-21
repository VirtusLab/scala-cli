package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class Java(
  home: Option[String] = None
)

object Java {
  implicit val reader: ConfigReader[Java] = deriveReader
}
