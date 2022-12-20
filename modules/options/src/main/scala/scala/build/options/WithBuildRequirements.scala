package scala.build.options

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
