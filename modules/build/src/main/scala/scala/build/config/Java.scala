package scala.build.config

import pureconfig.ConfigReader

import scala.build.config.reader.{DerivedConfigReader, Description}

final case class Java(
  @Description("Java home")
    home: Option[String] = None,
  @Description("Java options")
    options: List[String] = Nil
)

object Java {
  implicit val reader = DerivedConfigReader[Java]
}
