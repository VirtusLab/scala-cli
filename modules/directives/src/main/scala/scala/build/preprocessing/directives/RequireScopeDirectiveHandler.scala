package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, DirectiveErrors}
import scala.build.options.{BuildRequirements, Scope}
import scala.build.preprocessing.Scoped
import scala.build.Positioned

case object RequireScopeDirectiveHandler extends BuildRequirementsHandler(Single(DirectiveValue.String)) {
  def name        = "Scope"
  def description = "Require a scope for the current or provided file"
  def usagesCode = Seq(
    "//> using target.scope <scope>",
    "//> using target.scope [<scope> in <file>]+"
  )
  override def examples = Seq(
    "//> using target.scope \"test\"",
    "//> using target.scope \"test\" in \"tests\", \"main\" in \"src\""
  )

  def keys: Seq[String] = Seq("target.scope")

  val scopesByName = Scope.all.map(s => s.name -> s).toMap

  def process(value: Positioned[String])(using Ctx) =
    scopesByName.get(value.value) match
      case None =>
        val supported = scopesByName.values.map(v => s"\"$v\"").mkString(", ")
        value.error(s"Invalid scope, expected one of: $supported")
      case Some(scope) =>
        Right(BuildRequirements(scope = Some(BuildRequirements.ScopeRequirement(scope))))
}
