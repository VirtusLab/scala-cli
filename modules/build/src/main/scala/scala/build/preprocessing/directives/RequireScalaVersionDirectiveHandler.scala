package scala.build.preprocessing.directives

import scala.build.options.BuildRequirements

case object RequireScalaVersionDirectiveHandler extends RequireDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildRequirements]] =
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
