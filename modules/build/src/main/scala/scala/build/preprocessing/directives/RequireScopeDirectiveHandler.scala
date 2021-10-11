package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildRequirements, Scope}

case object RequireScopeDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scope"
  def description      = "Require a scope for the current file"
  def usage            = "using target _scope_"
  override def usageMd = "`using target `_scope_"
  override def examples = Seq(
    "using target test"
  )

  private val scopesByName = Scope.all.map(s => s.name -> s).toMap

  def handle(directive: Directive): Option[Either[BuildException, BuildRequirements]] =
    directive.values match {
      case Seq(name) if scopesByName.contains(name) =>
        val scope = scopesByName(name)
        val req = BuildRequirements(
          scope = Some(BuildRequirements.ScopeRequirement(scope))
        )
        Some(Right(req))
      case _ =>
        None
    }
}
