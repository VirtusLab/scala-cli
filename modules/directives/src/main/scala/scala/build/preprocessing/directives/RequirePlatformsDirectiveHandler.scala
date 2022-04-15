package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, MalformedPlatformError}
import scala.build.options.{BuildRequirements, Platform}
import scala.build.preprocessing.Scoped

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
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] =
    checkIfValuesAreExpected(scopedDirective) flatMap { groupedPositionedValuesContainer =>
      val stringValues                    = groupedPositionedValuesContainer.scopedStringValues
      val (nonScopedValues, scopedValues) = stringValues.partition(_.maybeScopePath.isEmpty)
      val nonScopedPlatforms =
        Option(nonScopedValues.map(v => Platform.normalize(v.positioned.value)))
          .filter(_.nonEmpty)

      val nonscoped: Either[MalformedPlatformError, Option[BuildRequirements]] =
        nonScopedPlatforms match {
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

      val scoped: Either[BuildException, Seq[Scoped[BuildRequirements]]] =
        scopedValues.groupBy(_.maybeScopePath.get).map {
          case (scopePath, list) =>
            val platforms = list.map(_.positioned.value).map(Platform.normalize)
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
