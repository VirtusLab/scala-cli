package scala.build.info

import scala.build.info.BuildInfo.escapeBackslashes
import scala.build.internal.Constants
import scala.build.options.*

final case class BuildInfo(
  scalaVersion: Option[String] = None,
  platform: Option[String] = None,
  jvmVersion: Option[String] = None,
  scalaJsVersion: Option[String] = None,
  jsEsVersion: Option[String] = None,
  scalaNativeVersion: Option[String] = None,
  mainClass: Option[String] = None,
  scopes: Map[String, ScopedBuildInfo] = Map.empty
) {
  def +(other: BuildInfo): BuildInfo =
    BuildInfo.monoid.orElse(this, other)

  def withScope(scopeName: String, scopedBuildInfo: ScopedBuildInfo): BuildInfo =
    if (scopedBuildInfo.sources.isEmpty)
      this.copy(
        scopes = this.scopes + (scopeName -> ScopedBuildInfo.empty)
      )
    else
      this.copy(
        scopes = this.scopes + (scopeName -> scopedBuildInfo)
      )

  def generateContents(): String = {
    val nl     = System.lineSeparator()
    val indent = " " * 2
    val stringVals = Seq(
      s"val scalaVersion = \"${escapeBackslashes(scalaVersion.getOrElse(Constants.defaultScalaVersion))}\"",
      s"val platform = \"${escapeBackslashes(platform.getOrElse(Platform.JVM.repr))}\""
    )

    val optionVals = Seq(
      "val jvmVersion ="         -> jvmVersion,
      "val scalaJsVersion ="     -> scalaJsVersion,
      "val jsEsVersion ="        -> jsEsVersion,
      "val scalaNativeVersion =" -> scalaNativeVersion,
      "val mainClass ="          -> mainClass
    ).map { case (prefix, opt) =>
      opt.map(v => s"$prefix Some(\"${escapeBackslashes(v)}\")").getOrElse(s"$prefix None")
    }

    val allVals = stringVals ++ optionVals

    val scopesContents =
      for ((scopeName, scopedBuildInfo) <- scopes)
        yield {
          val scopedBuildInfoVals = scopedBuildInfo.generateContentLines()
            .mkString(indent, nl + indent * 2, "")
          s"""${indent}object ${scopeName.capitalize} {
             |$indent$scopedBuildInfoVals
             |$indent}
             |""".stripMargin
        }

    s"""package scala.cli.build
       |
       |object BuildInfo {
       |${allVals.mkString(indent, nl + indent, nl)}
       |${scopesContents.mkString(nl, nl, "")}
       |}
       |""".stripMargin
  }
}

object BuildInfo {
  def apply(
    options: BuildOptions
  ): BuildInfo =
    Seq(
      BuildInfo(
        mainClass = options.mainClass
      ),
      scalaVersionSettings(options),
      platformSettings(options)
    )
      .reduceLeft(_ + _)

  def escapeBackslashes(s: String): String =
    s.replace("\\", "\\\\")

  private def scalaVersionSettings(options: BuildOptions): BuildInfo = {
    val sv = options.scalaParams.toOption.flatten
      .map(_.scalaVersion)
      .getOrElse(Constants.defaultScalaVersion)

    BuildInfo(scalaVersion = Some(sv))
  }

  private def scalaJsSettings(options: ScalaJsOptions): BuildInfo = {

    val scalaJsVersion = Some(options.version.getOrElse(Constants.scalaJsVersion))

    BuildInfo(
      platform = Some(Platform.JS.repr),
      scalaJsVersion = scalaJsVersion,
      jsEsVersion = options.esVersionStr
    )
  }

  private def scalaNativeSettings(options: ScalaNativeOptions): BuildInfo = {
    val scalaNativeVersion = Some(options.version.getOrElse(Constants.scalaNativeVersion))

    BuildInfo(
      platform = Some(Platform.Native.repr),
      scalaNativeVersion = scalaNativeVersion
    )
  }

  private def jvmSettings(options: BuildOptions): BuildInfo =
    BuildInfo(
      platform = Some(Platform.JVM.repr),
      jvmVersion = options.javaOptions.jvmIdOpt.map(_.value)
        .orElse(Some(options.javaHome().value.version.toString))
    )

  private def platformSettings(options: BuildOptions): BuildInfo =
    options.scalaOptions.platform.map(_.value) match {
      case Some(Platform.JS) =>
        scalaJsSettings(options.scalaJsOptions)
      case Some(Platform.Native) =>
        scalaNativeSettings(options.scalaNativeOptions)
      case _ => jvmSettings(options)
    }

  implicit val monoid: ConfigMonoid[BuildInfo] = ConfigMonoid.derive
}
