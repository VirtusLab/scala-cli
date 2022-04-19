package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.options.{BuildRequirements, Scope}
import scala.build.preprocessing.Scoped

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

  override def getValueNumberBounds(key: String): UsingDirectiveValueNumberBounds =
    UsingDirectiveValueNumberBounds(1, 1)

  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] =
    checkIfValuesAreExpected(scopedDirective) flatMap { groupedPositionedValuesContainer =>
      val values         = groupedPositionedValuesContainer.scopedStringValues
      val nonScopedValue = values.find(v => v.maybeScopePath.isEmpty)

      val nonscoped = nonScopedValue match {
        case None => Right(None)
        case Some(ScopedValue(positioned, _)) if scopesByName.contains(positioned.value) =>
          val scope = scopesByName(positioned.value)
          val req = BuildRequirements(
            scope = Some(BuildRequirements.ScopeRequirement(scope))
          )
          Right(Some(req))
        case _ => Left(new DirectiveErrors(::("No such scope", Nil), Seq.empty))
      }

      val scoped = values
        .collect {
          case ScopedValue(positioned, Some(scopePath))
              if scopesByName.contains(positioned.value) =>
            val scope = scopesByName(positioned.value)
            val req = Scoped(
              scopePath,
              BuildRequirements(
                scope = Some(BuildRequirements.ScopeRequirement(scope))
              )
            )
            Right(req)
          case ScopedValue(_, Some(_)) =>
            Left(new DirectiveErrors(::("No such scope", Nil), Seq.empty))
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
