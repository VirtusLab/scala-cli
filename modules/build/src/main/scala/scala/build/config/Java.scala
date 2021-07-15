package scala.build.config

import pureconfig.ConfigReader

import scala.build.config.reader.{DerivedConfigReader, Description}

final case class Java(
  @Description("Java home")
    home: Option[String] = None
)

object Java {
  implicit val reader = DerivedConfigReader[Java]
}
