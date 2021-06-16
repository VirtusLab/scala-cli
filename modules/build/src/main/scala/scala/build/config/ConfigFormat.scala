package scala.build.config

import pureconfig.ConfigReader

import pureconfig.generic.semiauto._
import scala.build.BuildOptions

final case class ConfigFormat(
  scala: ConfigFormat.Scala = ConfigFormat.Scala()
) {
  def buildOptions: BuildOptions =
    BuildOptions(
      scalaVersion = scala.version,
      scalaBinaryVersion = scala.binaryVersion
    )
}

object ConfigFormat {

  final case class Scala(
    version: Option[String] = None,
    binaryVersion: Option[String] = None
  )
  object Scala {
    implicit val reader: ConfigReader[Scala] = deriveReader
  }

  implicit val reader: ConfigReader[ConfigFormat] = deriveReader
}
