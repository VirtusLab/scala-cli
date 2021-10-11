package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements

case object RequireScalaVersionDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scala version"
  def description      = "Require a Scala version for the current file"
  def usage            = "require scala _version_"
  override def usageMd = "`require scala `_version_"
  override def examples = Seq(
    "require scala 3",
    "require scala 2.13",
    "require scala 3.0.2"
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildRequirements]] =
    directive.values match {
      case Seq("scala", ">=", minVer) =>
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionHigherThan(minVer, orEqual = true))
        )
        Some(Right(req))
      case Seq("scala", "<=", maxVer) =>
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionLowerThan(maxVer, orEqual = true))
        )
        Some(Right(req))
      case Seq("scala", "==", reqVer) =>
        // FIXME What about things like just '2.12'?
        val req = BuildRequirements(
          scalaVersion = Seq(BuildRequirements.VersionEquals(reqVer, loose = true))
        )
        Some(Right(req))
      case _ =>
        None
    }
}
