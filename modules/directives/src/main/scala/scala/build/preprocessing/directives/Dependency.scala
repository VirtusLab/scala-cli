package scala.build.preprocessing.directives

import dependency.AnyDependency

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{
  BuildOptions,
  ClassPathOptions,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.DirectiveUtil.*
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using dep com.lihaoyi::os-lib:0.9.1")
@DirectiveExamples("//> using test.dep org.scalatest::scalatest:3.2.10")
@DirectiveExamples("//> using test.dep org.scalameta::munit:0.7.29")
@DirectiveExamples(
  "//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar"
)
@DirectiveUsage(
  "//> using dep org:name:ver | //> using deps org:name:ver org2:name2:ver2",
  "`//> using dep `_org_`:`name`:`ver"
)
@DirectiveDescription("Add dependencies")
@DirectiveLevel(SpecificationLevel.MUST)
final case class Dependency(
  @DirectiveName("lib")  // backwards compat
  @DirectiveName("libs") // backwards compat
  @DirectiveName("dep")
  @DirectiveName("deps")
  @DirectiveName("dependencies")
  dependency: List[Positioned[String]] = Nil,
  @DirectiveName("test.dep")
  @DirectiveName("test.deps")
  @DirectiveName("test.dependencies")
  testDependency: List[Positioned[String]] = Nil,
  @DirectiveName("compileOnly.lib")
  @DirectiveName("compileOnly.libs")
  @DirectiveName("compileOnly.dep")
  @DirectiveName("compileOnly.deps")
  @DirectiveName("compileOnly.dependencies")
  compileOnlyDependency: List[Positioned[String]] = Nil
) extends HasBuildOptionsWithRequirements {
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = List(
    Dependency.buildOptions(dependency).map(_.withEmptyRequirements),
    Dependency.buildOptions(testDependency).map(_.withScopeRequirement(Scope.Test)),
    Dependency.buildOptions(compileOnlyDependency, isCompileOnly = true)
      .map(_.withEmptyRequirements)
  )
}

object Dependency {
  val handler: DirectiveHandler[Dependency] = DirectiveHandler.derive
  def buildOptions(
    ds: List[Positioned[String]],
    isCompileOnly: Boolean = false
  ): Either[BuildException, BuildOptions] = either {
    val dependencies: ShadowingSeq[Positioned[AnyDependency]] =
      value(ds.asDependencies.map(ShadowingSeq.from))
    val classPathOptions =
      if (isCompileOnly) ClassPathOptions(extraCompileOnlyDependencies = dependencies)
      else ClassPathOptions(extraDependencies = dependencies)
    BuildOptions(classPathOptions = classPathOptions)
  }
}
