package scala.build.options

final case class HasScope[+T](
  scope: Scope,
  value: T
) {
  def valueFor(currentScope: Scope): Option[T] =
    if (currentScope == scope) Some(value)
    else None
  def valueForInheriting(currentScope: Scope): Option[T] =
    if (currentScope.allScopes.contains(scope)) Some(value)
    else None
  def map[U](f: T => U): HasScope[U] =
    copy(value = f(value))
}
