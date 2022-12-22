package scala.build.preprocessing

import scala.build.errors.BuildException

final case class Scoped[+T](path: ScopePath, value: T) {
  def appliesTo(candidate: ScopePath): Boolean =
    path.root == candidate.root &&
    candidate.subPath.startsWith(path.subPath)
  def valueFor(candidate: ScopePath): Option[T] =
    if (appliesTo(candidate)) Some(value) else None

  def map[U](f: T => U): Scoped[U] =
    copy(value = f(value))
  def mapE[U](f: T => Either[BuildException, U]): Either[BuildException, Scoped[U]] =
    f(value).map(u => copy(value = u))
}
