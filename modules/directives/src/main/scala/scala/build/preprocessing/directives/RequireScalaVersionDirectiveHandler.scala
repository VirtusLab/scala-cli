package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.options.BuildRequirements
import scala.build.preprocessing.Scoped
import scala.build.preprocessing.directives.UsingDirectiveValueKind.UsingDirectiveValueKind

case object RequireScalaVersionDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scala version"
  def description      = "Require a Scala version for the current file"
  def usage            = "//> using target.scala _version_"
  override def usageMd = "`//> using target.scala `_version_"
  override def examples = Seq(
    "//> using target.scala \"3\"",
    "//> using target.scala.>= \"2.13\"",
    "//> using target.scala.< \"3.0.2\""
  )

  def keys: Seq[String] = Seq(
    "target.scala.==",
    "target.scala.>=",
    "target.scala.<=",
    "target.scala.>",
    "target.scala.<",
    "target.scala"
  )

  override def getSupportedTypes(key: String): Set[UsingDirectiveValueKind] =
    Set(UsingDirectiveValueKind.STRING, UsingDirectiveValueKind.NUMERIC)

  private def handleVersion(
    key: String,
    v: String
  ): Either[BuildException, Option[BuildRequirements]] = key match {
    case "target.scala" | "target.scala.==" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionEquals(v, loose = true))
      )
      Right(Some(req))
    case "target.scala.>" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionHigherThan(v, orEqual = false))
      )
      Right(Some(req))
    case "target.scala.<" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionLowerThan(v, orEqual = false))
      )
      Right(Some(req))
    case "target.scala.>=" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionHigherThan(v, orEqual = true))
      )
      Right(Some(req))
    case "target.scala.<=" =>
      val req = BuildRequirements(
        scalaVersion = Seq(BuildRequirements.VersionLowerThan(v, orEqual = true))
      )
      Right(Some(req))
    case _ =>
      // TODO: Handle errors and conflicts
      Left(new DirectiveErrors(::("Match error in ScalaVersionDirectiveHandler", Nil), Seq.empty))
  }

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap {
      groupedPositionedValuesContainer =>

        val (scopedValues, nonScopedValues) =
          DirectiveUtil.partitionBasedOnHavingScope(groupedPositionedValuesContainer)

        val nonScopedBuildRequirements = nonScopedValues.headOption match {
          case None => Right(None)
          case Some(ScopedValue(positioned, None)) =>
            handleVersion(scopedDirective.directive.key, positioned.value)
        }

        val scopedBuildRequirements = scopedValues.map {
          case ScopedValue(positioned, Some(scopePath)) => handleVersion(
              scopedDirective.directive.key,
              positioned.value
            ).map(_.map(buildRequirements => Scoped(scopePath, buildRequirements)))
        }
          .sequence
          .left.map(CompositeBuildException(_))
          .map(_.flatten)

        (nonScopedBuildRequirements, scopedBuildRequirements)
          .traverseN
          .left.map(CompositeBuildException(_))
          .map {
            case (ns, s) => ProcessedDirective(ns, s)
          }
    }

}
