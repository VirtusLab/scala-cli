package scala.build.preprocessing

final case class Scoped[T](path: ScopePath, value: T) {
  def appliesTo(candidate: ScopePath): Boolean =
    path.root == candidate.root &&
    candidate.path.startsWith(path.path)
  def valueFor(candidate: ScopePath): Option[T] =
    if (appliesTo(candidate)) Some(value) else None

  def mapEither[E, U](op: T => Either[E, U]): Either[E, Scoped[U]] =
    op(value).map(v => copy(value = v))
}
