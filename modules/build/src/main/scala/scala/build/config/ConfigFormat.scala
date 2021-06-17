package scala.build.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

import scala.build.Build
import scala.build.options.BuildOptions
import scala.build.options.ScalaOptions
import scala.build.options.JavaOptions

final case class ConfigFormat(
  scala: Scala = Scala(),
  scalaJs: ScalaJs = ScalaJs(),
  jvm: Option[String] = None,
  java: Java = Java()
) {
  def buildOptions: BuildOptions =
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = scala.version,
        scalaBinaryVersion = scala.binaryVersion
      ),
      javaOptions = JavaOptions(
        javaHomeOpt = java.home,
        jvmIdOpt = jvm
      )
    )
}

object ConfigFormat {
  implicit val reader: ConfigReader[ConfigFormat] = deriveReader
}
