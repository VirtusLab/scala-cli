package scala.build.options

final case class BuildRequirements(
  scalaVersion: Seq[BuildRequirements.VersionRequirement] = Nil,
  platform: Seq[BuildRequirements.PlatformRequirement] = Nil
) {
  def withScalaVersion(sv: String): Either[String, BuildRequirements] = {
    val dontPass = scalaVersion.filter(!_.valid(sv))
    if (dontPass.isEmpty)
      Right(copy(scalaVersion = Nil))
    else
      Left(dontPass.map(_.failedMessage).mkString(", "))
  }
  def withPlatform(pf: Platform): Either[String, BuildRequirements] =
    BuildRequirements.PlatformRequirement.merge(platform) match {
      case None => Right(this)
      case Some(platform0) =>
        if (platform0.valid(pf)) Right(copy(platform = Nil))
        else Left(platform0.failedMessage)
    }
  def isEmpty: Boolean =
    this == BuildRequirements()
  def orElse(other: BuildRequirements): BuildRequirements =
    BuildRequirements.monoid.orElse(this, other)
}

object BuildRequirements {

  sealed trait VersionRequirement extends Product with Serializable {
    def valid(version: String): Boolean
    def failedMessage: String
  }

  final case class VersionEquals(requiredVersion: String, loose: Boolean)
      extends VersionRequirement {
    def looselyValid(version: String): Boolean =
      version == requiredVersion ||
      version.startsWith(requiredVersion + ".") ||
      version.startsWith(requiredVersion + "-")
    def strictlyValid(version: String): Boolean = {
      val cmp = coursier.core.Version(requiredVersion).compare(coursier.core.Version(version))
      cmp == 0
    }
    def valid(version: String): Boolean =
      (loose && looselyValid(version)) || strictlyValid(version)
    def failedMessage: String = s"Expected version $requiredVersion"
  }
  final case class VersionLowerThan(maxVersion: String, orEqual: Boolean)
      extends VersionRequirement {
    def valid(version: String): Boolean = {
      val cmp = coursier.core.Version(version).compare(coursier.core.Version(maxVersion))
      cmp < 0 || (orEqual && cmp == 0)
    }
    def failedMessage: String =
      if (orEqual) s"Expected version lower than or equal to $maxVersion"
      else s"Expected version lower than $maxVersion"
  }
  final case class VersionHigherThan(minVersion: String, orEqual: Boolean)
      extends VersionRequirement {
    def valid(version: String): Boolean = {
      val cmp = coursier.core.Version(minVersion).compare(coursier.core.Version(version))
      cmp < 0 || (orEqual && cmp == 0)
    }
    def failedMessage: String =
      if (orEqual) s"Expected version higher than or equal to $minVersion"
      else s"Expected version higher than $minVersion"
  }

  final case class PlatformRequirement(platforms: Set[Platform]) {
    def valid(pf: Platform): Boolean =
      platforms.contains(pf)
    def failedMessage: String =
      "Expected platform: " + platforms.toVector.map(_.repr).sorted.mkString(" or ")
  }

  object PlatformRequirement {
    def merge(requirements: Seq[PlatformRequirement]): Option[PlatformRequirement] =
      if (requirements.isEmpty) None
      else if (requirements.lengthCompare(1) == 0) Some(requirements.head)
      else {
        val platforms = requirements.tail.foldLeft(requirements.head.platforms) { (acc, req) =>
          acc.intersect(req.platforms)
        }
        Some(PlatformRequirement(platforms))
      }
  }

  implicit val monoid: ConfigMonoid[BuildRequirements] = ConfigMonoid.derive

}
