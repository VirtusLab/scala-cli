package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

import scala.build.{Build, BuildOptions}

final case class ConfigFormat(
  scala: ConfigFormat.Scala = ConfigFormat.Scala(),
  scalaJs: ConfigFormat.ScalaJs = ConfigFormat.ScalaJs(),
  javaHome: Option[String] = None,
  jvm: Option[String] = None
) {
  def buildOptions: BuildOptions =
    BuildOptions(
      scalaVersion = scala.version,
      scalaBinaryVersion = scala.binaryVersion,
      javaHomeOpt = javaHome,
      jvmIdOpt = jvm
    )
}

object ConfigFormat {

  final case class Scala(
    version: Option[String] = None,
    binaryVersion: Option[String] = None,
    platform: Option[String] = None
  )
  object Scala {
    implicit val reader: ConfigReader[Scala] = deriveReader
  }

  final case class ScalaJs(
    version: Option[String] = None,
    suffix: Option[String] = None
  )
  object ScalaJs {
    implicit val reader: ConfigReader[ScalaJs] = deriveReader
  }

  implicit val reader: ConfigReader[ConfigFormat] = deriveReader
}
