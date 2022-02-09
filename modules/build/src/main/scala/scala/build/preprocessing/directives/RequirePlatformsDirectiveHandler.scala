package scala.build.preprocessing.directives
import os.Path

import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, MalformedPlatformError}
import scala.build.options.{BuildRequirements, Platform}
import scala.build.preprocessing.{ScopePath, Scoped}

case object RequirePlatformsDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Platform"
  def description      = "Require a Scala platform for the current file"
  def usage            = "//> using target.platform _platform_"
  override def usageMd = "`//> using target.platform `_platform_"
  override def examples = Seq(
    "//> using target.platform \"scala-js\"",
    "//> using target.platform \"scala-js\", \"scala-native\"",
    "//> using target.platform \"jvm\""
  )

  def keys: Seq[String] = Seq(
    "target.platform"
  )

  def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] = {
    val values          = DirectiveUtil.stringValues(directive.values, path, cwd)
    val nonscopedValues = values.filter(_._2.isEmpty)
    val scopedValues    = values.collect { case (v, Some(scope)) => (v, scope) }
    val nonScopedPlatforms = Option(nonscopedValues.map(v => Platform.normalize(v._1.value)))
      .filter(_.nonEmpty)

    val nonscoped = nonScopedPlatforms match {
      case Some(platforms) =>
        val parsed = Platform.parseSpec(platforms)
        parsed match {
          case None => Left(new MalformedPlatformError(platforms.mkString(", ")))
          case Some(p) => Right(Some(BuildRequirements(
              platform = Seq(BuildRequirements.PlatformRequirement(p))
            )))
        }
      case None => Right(None)
    }

    val scoped = scopedValues.groupBy(_._2).map {
      case (scopePath, list) =>
        val platforms = list.map(_._1.value).map(Platform.normalize)
        val parsed    = Platform.parseSpec(platforms)
        parsed match {
          case None => Left(new MalformedPlatformError(platforms.mkString(", ")))
          case Some(p) => Right(Seq(Scoped(
              scopePath,
              BuildRequirements(
                platform = Seq(BuildRequirements.PlatformRequirement(p))
              )
            )))
        }
    }
      .toSeq
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.flatten)

    (nonscoped, scoped)
      .traverseN
      .left.map(CompositeBuildException(_))
      .map {
        case (ns, s) => ProcessedDirective(ns, s)
      }
  }
}
