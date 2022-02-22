package scala.build.preprocessing.directives

import os.Path

import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.options.{BuildRequirements, Scope}
import scala.build.preprocessing.{ScopePath, Scoped}

case object RequireScopeDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Scope"
  def description      = "Require a scope for the current file"
  def usage            = "//> using target.scope _scope_"
  override def usageMd = "`//> using target.scope `_scope_"
  override def examples = Seq(
    "//> using target.scope \"test\""
  )

  def keys: Seq[String] = Seq(
    "target.scope"
  )

  private val scopesByName = Scope.all.map(s => s.name -> s).toMap

  def handleValues(
    directive: StrictDirective,
    path: Either[String, Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] = {
    val values         = DirectiveUtil.stringValues(directive.values, path, cwd)
    val nonscopedValue = values.find(v => v._2.isEmpty)

    val nonscoped = nonscopedValue match {
      case None => Right(None)
      case Some((name, _)) if scopesByName.contains(name.value) =>
        val scope = scopesByName(name.value)
        val req = BuildRequirements(
          scope = Some(BuildRequirements.ScopeRequirement(scope))
        )
        Right(Some(req))
      case _ => Left(new DirectiveErrors(::("No such scope", Nil), Seq.empty))
    }

    val scoped = values
      .collect {
        case (name, Some(scopePath)) if scopesByName.contains(name.value) =>
          val scope = scopesByName(name.value)
          val req = Scoped(
            scopePath,
            BuildRequirements(
              scope = Some(BuildRequirements.ScopeRequirement(scope))
            )
          )
          Right(req)
        case (_, Some(_)) => Left(new DirectiveErrors(::("No such scope", Nil), Seq.empty))
      }
      .toSeq
      .sequence
      .left.map(CompositeBuildException(_))

    (nonscoped, scoped)
      .traverseN
      .left.map(CompositeBuildException(_))
      .map {
        case (ns, s) => ProcessedDirective(ns, s)
      }
  }
}
