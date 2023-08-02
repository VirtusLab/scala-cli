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
      "/** version of Scala used to compile this project */",
      s"val scalaVersion = \"${escapeBackslashes(scalaVersion.getOrElse(Constants.defaultScalaVersion))}\"",
      "/** target platform of this project, it can be \"JVM\" or \"JS\" or \"Native\" */",
      s"val platform = \"${escapeBackslashes(platform.getOrElse(Platform.JVM.repr))}\""
    )

    val optionVals = Seq(
      Seq(
        "/** version of JVM, if it's the target platform */",
        "val jvmVersion ="
      ) -> jvmVersion,
      Seq(
        "/** version of Scala.js, if it's the target platform */",
        "val scalaJsVersion ="
      ) -> scalaJsVersion,
      Seq(
        "/** Scala.js ECMA Script version, if Scala.js is the target platform */",
        "val jsEsVersion ="
      ) -> jsEsVersion,
      Seq(
        "/** version of Scala Native, if it's the target platform */",
        "val scalaNativeVersion ="
      ) -> scalaNativeVersion,
      Seq(
        "/** Main class specified for the project */",
        "val mainClass ="
      ) -> mainClass
    ).flatMap { case (Seq(scaladoc, prefix), opt) =>
      Seq(
        scaladoc,
        opt.map(v => s"$prefix Some(\"${escapeBackslashes(v)}\")").getOrElse(s"$prefix None")
      )
    }

    val allVals = stringVals ++ optionVals

    val scopesContents =
      for ((scopeName, scopedBuildInfo) <- scopes)
        yield {
          val scopedBuildInfoVals = scopedBuildInfo.generateContentLines()
            .mkString(indent, nl + indent * 2, "")
          s"""$indent/** Information about the ${scopeName.capitalize} scope */
             |${indent}object ${scopeName.capitalize} {
             |$indent$scopedBuildInfoVals
             |$indent}""".stripMargin
        }

    s"""package scala.cli.build
       |
       |/** Information about the build gathered by Scala CLI */
       |object BuildInfo {
       |${allVals.mkString(indent, nl + indent, nl)}
       |${scopesContents.mkString(nl * 2)}
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
