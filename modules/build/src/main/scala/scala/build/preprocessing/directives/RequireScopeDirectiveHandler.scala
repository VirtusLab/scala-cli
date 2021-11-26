package scala.build.preprocessing.directives

import os.Path

import scala.build.errors.{BuildException, DirectiveErrors}
import scala.build.options.{BuildRequirements, Scope}
import scala.build.preprocessing.{ScopePath, Scoped}

case object RequireScopeDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scope"
  def description      = "Require a scope for the current file"
  def usage            = "using target _scope_"
  override def usageMd = "`using target `_scope_"
  override def examples = Seq(
    "using target test"
  )

  override def keys: Seq[String] = Seq(
    "target.scope"
  )

  private val scopesByName = Scope.all.map(s => s.name -> s).toMap

  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]] =
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

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildRequirements], Seq[Scoped[BuildRequirements]])] = {
    val values         = DirectiveUtil.stringValues(directive.values, path, cwd)
    val nonscopedValue = values.find(v => v._3.isEmpty)

    val nonscoped =
      nonscopedValue.fold[Either[BuildException, Option[BuildRequirements]]](Right(None)) {
        case (name, _, _) if scopesByName.contains(name) =>
          val scope = scopesByName(name)
          val req = BuildRequirements(
            scope = Some(BuildRequirements.ScopeRequirement(scope))
          )
          Right(Some(req))
        case _ => Left(new DirectiveErrors(::("No such scope", Nil)))
      }

    val scoped = values.filter(_._3.nonEmpty).map {
      case (name, _, Some(scopePath)) if scopesByName.contains(name) =>
        val scope = scopesByName(name)
        val req = Scoped(
          scopePath,
          BuildRequirements(
            scope = Some(BuildRequirements.ScopeRequirement(scope))
          )
        )
        Right(req)
      case _ => Left(new DirectiveErrors(::("No such scope", Nil)))
    }.foldLeft[Either[BuildException, Seq[Scoped[BuildRequirements]]]](Right(Seq.empty)) {
      case (Right(seq), Right(v)) => Right(seq :+ v)
      case (Left(err), _)         => Left(err)
      case (_, Left(err))         => Left(err)
    }

    for {
      ns <- nonscoped
      s  <- scoped
    } yield ns -> s
  }
}
