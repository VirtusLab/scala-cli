package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ScopePath

case object RequireScalaVersionDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scala version"
  def description      = "Require a Scala version for the current file"
  def usage            = "using target scala _version_"
  override def usageMd = "`using target scala `_version_"
  override def examples = Seq(
    "using target scala 3",
    "using target scala 2.13",
    "using target scala 3.0.2"
  )

  override def keys: Seq[String] = Seq.empty

  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]] =
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
