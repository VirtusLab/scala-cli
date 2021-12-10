package scala.build.preprocessing.directives
import os.Path

import scala.build.errors.{BuildException, MalformedPlatformError}
import scala.build.options.{BuildRequirements, Platform}
import scala.build.preprocessing.{ScopePath, Scoped}

case object RequirePlatformsDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Platform"
  def description      = "Require a Scala platform for the current file"
  def usage            = "// using target.platform _platform_"
  override def usageMd = "`// using target.platform `_platform_"
  override def examples = Seq(
    "// using target.platform \"scala-js\"",
    "// using target.platform \"scala-js\", \"scala-native\"",
    "// using target.platform \"jvm\""
  )

  override def keys: Seq[String] = Seq(
    "target.platform"
  )

  override def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]] =
    Platform.parseSpec(directive.values.map(Platform.normalize)) match {
      case Some(platforms) =>
        val reqs = BuildRequirements(
          platform = Seq(BuildRequirements.PlatformRequirement(platforms))
        )
        Some(Right(reqs))
      case None =>
        None
    }

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedRequireDirective] = {
    val values          = DirectiveUtil.stringValues(directive.values, path, cwd)
    val nonscopedValues = values.filter(_._3.isEmpty)
    val scopedValues    = values.filter(_._3.nonEmpty)
    val nonScopedPlatforms = Option(nonscopedValues.map(v => Platform.normalize(v._1)))
      .filter(_.nonEmpty)

    val nonscoped =
      nonScopedPlatforms.fold[Either[BuildException, Option[BuildRequirements]]](Right(None)) {
        platforms =>
          val parsed = Platform.parseSpec(platforms)
          parsed.fold[Either[BuildException, Option[BuildRequirements]]](
            Left(new MalformedPlatformError(platforms.mkString(", ")))
          ) { p =>
            Right(Some(BuildRequirements(
              platform = Seq(BuildRequirements.PlatformRequirement(p))
            )))
          }
      }
    val scoped = scopedValues.groupBy(_._3).map {
      case (Some(scopePath), list) =>
        val platforms = list.map(_._1).map(Platform.normalize)
        val parsed    = Platform.parseSpec(platforms)
        parsed.fold[Either[BuildException, Seq[Scoped[BuildRequirements]]]](
          Left(new MalformedPlatformError(platforms.mkString(", ")))
        ) { p =>
          Right(Seq(Scoped(
            scopePath,
            BuildRequirements(
              platform = Seq(BuildRequirements.PlatformRequirement(p))
            )
          )))
        }
    }.foldLeft[Either[BuildException, Seq[Scoped[BuildRequirements]]]](Right(Seq.empty)) {
      case (Right(seq), Right(v)) => Right(seq ++ v)
      case (Left(err), _)         => Left(err)
      case (_, Left(err))         => Left(err)
    }

    for {
      ns <- nonscoped
      s  <- scoped
    } yield ProcessedDirective(ns, s)
  }
}
