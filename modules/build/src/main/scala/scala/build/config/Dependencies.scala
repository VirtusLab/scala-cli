package scala.build.config

import pureconfig.ConfigReader

import scala.build.config.reader.DerivedConfigReader

final case class Dependencies(
  dependencies: List[String] = Nil,
  repositories: List[String] = Nil
)

object Dependencies {
  implicit val reader = DerivedConfigReader[Dependencies]
}
