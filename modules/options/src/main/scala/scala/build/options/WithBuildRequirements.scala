package scala.build.options

import scala.build.options.BuildRequirements.ScopeRequirement

final case class WithBuildRequirements[+T](
  requirements: BuildRequirements,
  value: T
) {
  def needsScalaVersion: Boolean =
    requirements.needsScalaVersion
  def withScalaVersion(sv: MaybeScalaVersion): Either[String, WithBuildRequirements[T]] =
    requirements.withScalaVersion(sv).map { updatedRequirements =>
      copy(requirements = updatedRequirements)
    }
  def withPlatform(pf: Platform): Either[String, WithBuildRequirements[T]] =
    requirements.withPlatform(pf).map { updatedRequirements =>
      copy(requirements = updatedRequirements)
    }
  def scopedValue(defaultScope: Scope): HasScope[T] =
    HasScope(requirements.scope.map(_.scope).getOrElse(defaultScope), value)
  def map[U](f: T => U): WithBuildRequirements[U] =
    copy(value = f(value))
}

object WithBuildRequirements {
  extension [T](t: T) {
    def withBuildRequirements(buildRequirements: BuildRequirements): WithBuildRequirements[T] =
      WithBuildRequirements(buildRequirements, t)

    def withEmptyRequirements: WithBuildRequirements[T] =
      t.withBuildRequirements(BuildRequirements())

    def withScopeRequirement(scope: Scope): WithBuildRequirements[T] =
      t.withBuildRequirements(BuildRequirements(scope = Some(ScopeRequirement(scope = scope))))
  }
}
