package scala.build.bsp.buildtargets

import scala.build.options.Scope

final case class ProjectName(name: String) {
  def withScopeAppended(scope: Scope): ProjectName =
    if scope == Scope.Main then this else copy(name = s"$name-${scope.name.toLowerCase}")
}
