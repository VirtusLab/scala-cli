package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

final case class Dependencies(
  dependencies: List[String] = Nil,
  repositories: List[String] = Nil
)

object Dependencies {
  implicit val reader: ConfigReader[Dependencies] = deriveReader
}
